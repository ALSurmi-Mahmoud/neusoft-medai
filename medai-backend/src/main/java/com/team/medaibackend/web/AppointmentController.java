package com.team.medaibackend.web;

import com.team.medaibackend.entity.Appointment;
import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.AppointmentRepository;
import com.team.medaibackend.repository.PatientRepository;
import com.team.medaibackend.repository.UserRepository;
import com.team.medaibackend.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AppointmentController(AppointmentRepository appointmentRepository,
                                 PatientRepository patientRepository,
                                 UserRepository userRepository,
                                 AuditService auditService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // Get all appointments (for admin/nurse overview)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appointmentDate", "appointmentTime").ascending());
        Page<Appointment> appointmentPage = appointmentRepository.findAll(pageable);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment apt : appointmentPage.getContent()) {
            result.add(toDto(apt));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", result);
        response.put("totalElements", appointmentPage.getTotalElements());
        response.put("totalPages", appointmentPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // Get appointments for logged-in doctor
    @GetMapping("/doctor")
    public ResponseEntity<List<Map<String, Object>>> getDoctorAppointments(Authentication authentication) {
        String username = authentication.getName();
        User doctor = userRepository.findByUsername(username).orElse(null);

        if (doctor == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId());

        List<Map<String, Object>> result = appointments.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Get today's appointments for a doctor
    @GetMapping("/doctor/today")
    public ResponseEntity<List<Map<String, Object>>> getDoctorTodayAppointments(Authentication authentication) {
        String username = authentication.getName();
        User doctor = userRepository.findByUsername(username).orElse(null);

        if (doctor == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        LocalDate today = LocalDate.now();
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentDate(doctor.getId(), today);

        List<Map<String, Object>> result = appointments.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Get appointments for a specific week (for doctor calendar)
    @GetMapping("/doctor/week")
    public ResponseEntity<List<Map<String, Object>>> getDoctorWeekAppointments(
            @RequestParam String startDate,
            @RequestParam String endDate,
            Authentication authentication
    ) {
        String username = authentication.getName();
        User doctor = userRepository.findByUsername(username).orElse(null);

        if (doctor == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDateRange(
                doctor.getId(), start, end);

        List<Map<String, Object>> result = appointments.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Get today's appointments for nurse schedule
    @GetMapping("/today")
    public ResponseEntity<List<Map<String, Object>>> getTodayAppointments() {
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(today);

        List<Map<String, Object>> result = appointments.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Get patient's appointments
    @GetMapping("/patient")
    public ResponseEntity<List<Map<String, Object>>> getPatientAppointments(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Find patient record linked to this user
        Patient patient = patientRepository.findByUser_Email(user.getEmail()).orElse(null);

        List<Appointment> appointments;

        if (patient != null) {
            appointments = appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());
        } else {
            // If no patient record exists, return appointments created by this user (as patient)
            appointments = Collections.emptyList();
        }

        List<Map<String, Object>> result = appointments.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointment(@PathVariable Long id) {
        return appointmentRepository.findById(id)
                .map(apt -> ResponseEntity.ok(toDto(apt)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Updated create appointment to allow booking for patient without requiring patientId
    @PostMapping
    // Updated create appointment to allow booking for patient without requiring patientId
    public ResponseEntity<?> createAppointment(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Appointment apt = new Appointment();

            // Get current user
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username).orElse(null);

            // If patient is booking, link to their patient record
            if (currentUser != null && "PATIENT".equals(currentUser.getPrimaryRole())) {
                // First try to find by patientId (P-{userId})
                String expectedPatientId = "P-" + currentUser.getId();
                Patient patient = patientRepository.findByPatientId(expectedPatientId).orElse(null);

                // If not found by patientId, try by user email relationship
                if (patient == null) {
                    patient = patientRepository.findByUser_Email(currentUser.getEmail()).orElse(null);
                }

                // If still not found, create a new patient record
                if (patient == null) {
                    patient = new Patient();
                    patient.setPatientId(expectedPatientId);
                    patient.setName(currentUser.getFullName());
                    patient.setEmail(currentUser.getEmail());
                    patient.setUser(currentUser);  // Important: set the user relationship!
                    patient = patientRepository.save(patient);
                }
                apt.setPatient(patient);
            } else if (request.containsKey("patientId")) {
                // Staff booking for a patient
                Long patientId = Long.valueOf(request.get("patientId").toString());
                Patient patient = patientRepository.findById(patientId).orElse(null);
                apt.setPatient(patient);
            }

            // Set doctor - REQUIRED
            if (request.containsKey("doctorId")) {
                Long doctorId = Long.valueOf(request.get("doctorId").toString());
                User doctor = userRepository.findById(doctorId).orElse(null);
                if (doctor == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Doctor not found with ID: " + doctorId));
                }
                apt.setDoctor(doctor);
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "doctorId is required"));
            }

            // Set date and time - accept both "date"/"time" and "appointmentDate"/"appointmentTime"
            String dateStr = request.containsKey("date") ?
                    request.get("date").toString() :
                    request.containsKey("appointmentDate") ?
                            request.get("appointmentDate").toString() : null;

            String timeStr = request.containsKey("time") ?
                    request.get("time").toString() :
                    request.containsKey("appointmentTime") ?
                            request.get("appointmentTime").toString() : null;

            if (dateStr == null || timeStr == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Date and time are required"));
            }

            apt.setAppointmentDate(LocalDate.parse(dateStr));
            apt.setAppointmentTime(LocalTime.parse(timeStr));

            // Set optional fields
            if (request.containsKey("type")) {
                apt.setAppointmentType((String) request.get("type"));
            }
            if (request.containsKey("location")) {
                apt.setLocation((String) request.get("location"));
            }
            if (request.containsKey("notes")) {
                apt.setNotes((String) request.get("notes"));
            }
            if (request.containsKey("reason")) {
                apt.setReason((String) request.get("reason"));
            }
            if (request.containsKey("duration")) {
                apt.setDurationMinutes(Integer.valueOf(request.get("duration").toString()));
            } else {
                apt.setDurationMinutes(30);
            }

            // Calculate end time
            apt.setEndTime(apt.getAppointmentTime().plusMinutes(apt.getDurationMinutes()));

            apt.setStatus("scheduled");
            apt.setCreatedBy(currentUser != null ? currentUser.getId() : null);

            Appointment saved = appointmentRepository.save(apt);
            auditService.logAnonymous("CREATE", "APPOINTMENT", saved.getId().toString());

            return ResponseEntity.ok(Map.of(
                    "message", "Appointment booked successfully",
                    "id", saved.getId(),
                    "appointment", toDto(saved)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return appointmentRepository.findById(id).map(apt -> {
            // Set date and time - accept both "date"/"time" and "appointmentDate"/"appointmentTime"
            String dateStr = request.containsKey("date") ?
                    request.get("date").toString() :
                    request.containsKey("appointmentDate") ?
                            request.get("appointmentDate").toString() : null;

            String timeStr = request.containsKey("time") ?
                    request.get("time").toString() :
                    request.containsKey("appointmentTime") ?
                            request.get("appointmentTime").toString() : null;

            if (dateStr != null && timeStr != null) {
                apt.setAppointmentDate(LocalDate.parse(dateStr));
                apt.setAppointmentTime(LocalTime.parse(timeStr));
            }

            if (request.containsKey("type")) {
                apt.setAppointmentType((String) request.get("type"));
            }
            if (request.containsKey("status")) {
                apt.setStatus((String) request.get("status"));
            }
            if (request.containsKey("notes")) {
                apt.setNotes((String) request.get("notes"));
            }
            if (request.containsKey("location")) {
                apt.setLocation((String) request.get("location"));
            }
            if (request.containsKey("doctorId")) {
                Long doctorId = Long.valueOf(request.get("doctorId").toString());
                User doctor = userRepository.findById(doctorId).orElse(null);
                apt.setDoctor(doctor);
            }

            appointmentRepository.save(apt);
            auditService.logAnonymous("UPDATE", "APPOINTMENT", id.toString());

            return ResponseEntity.ok(Map.of("message", "Appointment updated", "appointment", toDto(apt)));
        }).orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
        }

        return appointmentRepository.findById(id).map(apt -> {
            apt.setStatus(newStatus);
            appointmentRepository.save(apt);
            auditService.logAnonymous("UPDATE_STATUS", "APPOINTMENT", id.toString());
            return ResponseEntity.ok(Map.of("message", "Status updated to " + newStatus));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id) {
        return appointmentRepository.findById(id).map(apt -> {
            appointmentRepository.delete(apt);
            auditService.logAnonymous("DELETE", "APPOINTMENT", id.toString());
            return ResponseEntity.ok(Map.of("message", "Appointment deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Get list of available doctors
    @GetMapping("/doctors")
    public ResponseEntity<List<Map<String, Object>>> getAvailableDoctors() {
        List<User> doctors = userRepository.findByRoleNameOrderByFullNameAsc("DOCTOR");

        List<Map<String, Object>> result = doctors.stream().map(doc -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", doc.getId());
            dto.put("name", doc.getFullName());
            dto.put("department", doc.getDepartment());
            dto.put("title", doc.getTitle());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // Stats endpoint
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAppointmentStats(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        LocalDate today = LocalDate.now();

        Map<String, Object> stats = new HashMap<>();

        if (user != null && "DOCTOR".equals(user.getPrimaryRole())) {
            stats.put("todayCount", appointmentRepository.countByDoctorIdAndAppointmentDate(user.getId(), today));
            stats.put("scheduledCount", appointmentRepository.countByDoctorIdAndStatus(user.getId(), "scheduled"));
            stats.put("completedToday", appointmentRepository.countByDoctorIdAndAppointmentDateAndStatus(user.getId(), today, "completed"));
        } else {
            stats.put("todayCount", appointmentRepository.countByAppointmentDate(today));
            stats.put("scheduledCount", appointmentRepository.countByStatus("scheduled"));
        }

        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> toDto(Appointment apt) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", apt.getId());
        dto.put("date", apt.getAppointmentDate() != null ? apt.getAppointmentDate().toString() : null);
        dto.put("time", apt.getAppointmentTime() != null ? apt.getAppointmentTime().toString() : null);
        dto.put("endTime", apt.getEndTime() != null ? apt.getEndTime().toString() : null);
        dto.put("type", apt.getAppointmentType());
        dto.put("status", apt.getStatus());
        dto.put("location", apt.getLocation());
        dto.put("notes", apt.getNotes());
        dto.put("reason", apt.getReason());
        dto.put("durationMinutes", apt.getDurationMinutes());

        if (apt.getPatient() != null) {
            dto.put("patientId", apt.getPatient().getId());
            dto.put("patientName", apt.getPatient().getName());
        }

        if (apt.getDoctor() != null) {
            dto.put("doctorId", apt.getDoctor().getId());
            dto.put("doctorName", apt.getDoctor().getFullName());
            dto.put("doctorDepartment", apt.getDoctor().getDepartment());
        }

        dto.put("createdAt", apt.getCreatedAt() != null ? apt.getCreatedAt().toString() : null);

        return dto;
    }
}
