package com.team.medaibackend.web;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import com.team.medaibackend.security.SecurityUtils;
import com.team.medaibackend.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReportRepository reportRepository;
    private final StudyRepository studyRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AuditService auditService;  // Add AuditService here

    public PatientController(UserRepository userRepository,
                             PatientRepository patientRepository,
                             AppointmentRepository appointmentRepository,
                             ReportRepository reportRepository,
                             StudyRepository studyRepository,
                             SecurityUtils securityUtils, PasswordEncoder passwordEncoder, RoleRepository roleRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.reportRepository = reportRepository;
        this.studyRepository = studyRepository;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    // ============================================================================
    // PATIENT SELF-SERVICE ENDPOINTS (Existing)
    // ============================================================================

    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Patient patient = patientRepository.findByUser_Email(user.getEmail()).orElse(null);

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("fullName", user.getFullName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());

        if (patient != null) {
            profile.put("patientId", patient.getPatientId());
            profile.put("dateOfBirth", patient.getBirthDate());
            profile.put("gender", patient.getSex());
        }

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> getMyAppointments(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Patient patient = patientRepository.findByUser_Email(user.getEmail()).orElse(null);

        if (patient == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Appointment> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());

        List<Map<String, Object>> result = appointments.stream().map(apt -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", apt.getId());
            dto.put("date", apt.getAppointmentDate().toString());
            dto.put("time", apt.getAppointmentTime().toString());
            dto.put("type", apt.getAppointmentType());
            dto.put("status", apt.getStatus());
            dto.put("location", apt.getLocation());
            dto.put("notes", apt.getNotes());

            if (apt.getDoctor() != null) {
                dto.put("doctorName", apt.getDoctor().getFullName());
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> getMyReports(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Patient patient = patientRepository.findByUser_Email(user.getEmail()).orElse(null);

        if (patient == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Report> reports = reportRepository.findByPatientId(patient.getId());

        List<Map<String, Object>> result = reports.stream().map(report -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", report.getId());
            dto.put("title", "Diagnostic Report #" + report.getId());
            dto.put("date", report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate().toString() : null);
            dto.put("status", report.getFinalized() ? "Final" : "Draft");
            dto.put("findings", report.getFindings());
            dto.put("impression", report.getImpression());
            dto.put("recommendations", report.getRecommendations());

            if (report.getAuthor() != null) {
                dto.put("doctorName", report.getAuthor().getFullName());
            }

            if (report.getStudy() != null) {
                dto.put("studyId", report.getStudy().getId());
                dto.put("modality", report.getStudy().getModality());
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        List<Patient> patients = patientRepository.findAll();

        List<Map<String, Object>> result = patients.stream().map(patient -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", patient.getId());
            dto.put("patientId", patient.getPatientId());
            dto.put("name", patient.getName());
            dto.put("sex", patient.getSex());
            dto.put("birthDate", patient.getBirthDate());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ============================================================================
    // NEW: DOCTOR'S PATIENT MANAGEMENT ENDPOINTS (Phase 4.1)
    // ============================================================================

    /**
     * Get list of patients for the logged-in doctor
     * Patients are identified by having appointments with this doctor
     */
    @GetMapping("/doctor/my-patients")
    public ResponseEntity<Map<String, Object>> getMyPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        // Get current doctor
        User doctor = securityUtils.getCurrentUserOrThrow();

        // Get all appointments for this doctor to find their patients
        List<Appointment> doctorAppointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId());

        // Extract unique patient IDs
        Set<Long> patientIds = doctorAppointments.stream()
                .map(apt -> apt.getPatient().getId())
                .collect(Collectors.toSet());

        // Get all patients for this doctor
        List<Patient> allPatients = patientRepository.findAllById(patientIds);

        // Apply filters
        List<Patient> filteredPatients = allPatients.stream()
                .filter(p -> {
                    // Search filter (name, ID, email)
                    if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matches =
                                (p.getName() != null && p.getName().toLowerCase().contains(searchLower)) ||
                                        (p.getPatientId() != null && p.getPatientId().toLowerCase().contains(searchLower)) ||
                                        (p.getEmail() != null && p.getEmail().toLowerCase().contains(searchLower));
                        if (!matches) return false;
                    }

                    // Gender filter
                    if (gender != null && !gender.isEmpty()) {
                        if (p.getSex() == null || !p.getSex().equalsIgnoreCase(gender)) {
                            return false;
                        }
                    }

                    // Age filter
                    if (ageMin != null || ageMax != null) {
                        if (p.getBirthDate() == null) return false;
                        int age = Period.between(p.getBirthDate(), LocalDate.now()).getYears();
                        if (ageMin != null && age < ageMin) return false;
                        if (ageMax != null && age > ageMax) return false;
                    }

                    // Status filter (active = visited in last 3 months)
                    if (status != null && !status.isEmpty()) {
                        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
                        boolean hasRecentAppointment = doctorAppointments.stream()
                                .anyMatch(apt ->
                                        apt.getPatient().getId().equals(p.getId()) &&
                                                apt.getAppointmentDate() != null &&
                                                apt.getAppointmentDate().isAfter(threeMonthsAgo)
                                );

                        if ("active".equals(status) && !hasRecentAppointment) return false;
                        if ("inactive".equals(status) && hasRecentAppointment) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Sort
        String sort = sortBy != null ? sortBy : "name";
        boolean ascending = !"desc".equalsIgnoreCase(sortDirection);

        filteredPatients.sort((p1, p2) -> {
            int comparison = 0;

            switch (sort) {
                case "name":
                    String name1 = p1.getName() != null ? p1.getName() : "";
                    String name2 = p2.getName() != null ? p2.getName() : "";
                    comparison = name1.compareToIgnoreCase(name2);
                    break;
                case "lastVisit":
                    LocalDate lastVisit1 = getLastVisitDate(p1.getId(), doctorAppointments);
                    LocalDate lastVisit2 = getLastVisitDate(p2.getId(), doctorAppointments);
                    if (lastVisit1 == null && lastVisit2 == null) comparison = 0;
                    else if (lastVisit1 == null) comparison = 1;
                    else if (lastVisit2 == null) comparison = -1;
                    else comparison = lastVisit1.compareTo(lastVisit2);
                    break;
                case "age":
                    Integer age1 = p1.getBirthDate() != null ?
                            Period.between(p1.getBirthDate(), LocalDate.now()).getYears() : 0;
                    Integer age2 = p2.getBirthDate() != null ?
                            Period.between(p2.getBirthDate(), LocalDate.now()).getYears() : 0;
                    comparison = age1.compareTo(age2);
                    break;
                default:
                    comparison = 0;
            }

            return ascending ? comparison : -comparison;
        });

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, filteredPatients.size());
        List<Patient> paginatedPatients = start < filteredPatients.size() ?
                filteredPatients.subList(start, end) : Collections.emptyList();

        // Build response with enhanced data
        List<Map<String, Object>> patientData = paginatedPatients.stream()
                .map(p -> buildPatientCardData(p, doctor.getId()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", patientData);
        response.put("totalElements", filteredPatients.size());
        response.put("totalPages", (int) Math.ceil((double) filteredPatients.size() / size));
        response.put("currentPage", page);
        response.put("pageSize", size);

        return ResponseEntity.ok(response);
    }

    /**
     * Get statistics for doctor's patients
     */
    @GetMapping("/doctor/patient-stats")
    public ResponseEntity<Map<String, Object>> getMyPatientStats() {
        User doctor = securityUtils.getCurrentUserOrThrow();

        // Get all appointments for this doctor
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId());

        // Extract unique patient IDs
        Set<Long> uniquePatientIds = appointments.stream()
                .map(apt -> apt.getPatient().getId())
                .collect(Collectors.toSet());

        int totalPatients = uniquePatientIds.size();

        // Active patients (visited in last 3 months)
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        long activePatients = appointments.stream()
                .filter(apt -> apt.getAppointmentDate() != null &&
                        apt.getAppointmentDate().isAfter(threeMonthsAgo))
                .map(apt -> apt.getPatient().getId())
                .distinct()
                .count();

        // New patients this month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        Set<Long> patientsThisMonth = appointments.stream()
                .filter(apt -> apt.getAppointmentDate() != null &&
                        apt.getAppointmentDate().isAfter(startOfMonth))
                .map(apt -> apt.getPatient().getId())
                .collect(Collectors.toSet());

        // Find truly new patients (first appointment this month)
        long newPatientsThisMonth = patientsThisMonth.stream()
                .filter(patientId -> {
                    LocalDate firstVisit = appointments.stream()
                            .filter(apt -> apt.getPatient().getId().equals(patientId))
                            .map(Appointment::getAppointmentDate)
                            .min(LocalDate::compareTo)
                            .orElse(null);
                    return firstVisit != null && firstVisit.isAfter(startOfMonth);
                })
                .count();

        // Patients needing follow-up (last visit > 6 months ago, status = completed)
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        long needingFollowUp = uniquePatientIds.stream()
                .filter(patientId -> {
                    LocalDate lastVisit = appointments.stream()
                            .filter(apt -> apt.getPatient().getId().equals(patientId))
                            .filter(apt -> "completed".equals(apt.getStatus()))
                            .map(Appointment::getAppointmentDate)
                            .max(LocalDate::compareTo)
                            .orElse(null);
                    return lastVisit != null && lastVisit.isBefore(sixMonthsAgo);
                })
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPatients", totalPatients);
        stats.put("activePatients", activePatients);
        stats.put("newPatientsThisMonth", newPatientsThisMonth);
        stats.put("needingFollowUp", needingFollowUp);
        stats.put("inactivePatients", totalPatients - activePatients);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get detailed patient information for doctor
     */
    @GetMapping("/doctor/patients/{id}")
    public ResponseEntity<?> getPatientDetail(@PathVariable Long id) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        Optional<Patient> patientOpt = patientRepository.findById(id);
        if (patientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Patient patient = patientOpt.get();

        // Verify this patient has appointments with this doctor
        boolean isMyPatient = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId())
                .stream()
                .anyMatch(apt -> apt.getPatient().getId().equals(id));

        if (!isMyPatient && !securityUtils.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        Map<String, Object> detail = buildPatientDetailData(patient, doctor.getId());

        return ResponseEntity.ok(detail);
    }
// ============================================================================
// ENHANCED PATIENT DETAIL ENDPOINT FOR PHASE 4.2
// Add this method to PatientController.java at line 415 (after the existing patients/{id} endpoint)
// ============================================================================

    /**
     * Get comprehensive patient detail with ALL appointments, reports, and studies
     * This is the ENHANCED version for Phase 4.2 Patient 360° Profile View
     */
    @GetMapping("/doctor/patients/{id}/complete")
    public ResponseEntity<?> getCompletePatientDetail(@PathVariable Long id) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        // Verify doctor role
        if (!"DOCTOR".equals(doctor.getPrimaryRole()) && !securityUtils.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify this patient has appointments with this doctor
        boolean isMyPatient = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId())
                .stream()
                .anyMatch(apt -> apt.getPatient().getId().equals(id));

        if (!isMyPatient && !securityUtils.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        // Build comprehensive patient data
        Map<String, Object> detail = new HashMap<>();

        // ===== PATIENT DEMOGRAPHICS =====
        detail.put("id", patient.getId());
        detail.put("patientId", patient.getPatientId());
        detail.put("name", patient.getName());
        detail.put("email", patient.getEmail());
        detail.put("phone", patient.getPhone());
        detail.put("birthDate", patient.getBirthDate());
        detail.put("sex", patient.getSex());
        detail.put("bloodType", patient.getBloodType());

        // Address
        detail.put("address", patient.getAddress());
        detail.put("city", patient.getCity());
        detail.put("state", patient.getState());
        detail.put("zipCode", patient.getZipCode());

        // Emergency Contact
        detail.put("emergencyContactName", patient.getEmergencyContactName());
        detail.put("emergencyContactPhone", patient.getEmergencyContactPhone());
        detail.put("emergencyContactRelationship", patient.getEmergencyContactRelationship());

        // Medical Information
        detail.put("allergies", patient.getAllergies());
        detail.put("medicalConditions", patient.getMedicalConditions());
        detail.put("currentMedications", patient.getCurrentMedications());

        // Calculate age
        if (patient.getBirthDate() != null) {
            int age = Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
            detail.put("age", age);
        } else {
            detail.put("age", null);
        }

        // ===== GET ALL RELATED DATA =====
        List<Appointment> allAppointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());

        List<Report> allReports = reportRepository.findByPatientId(patient.getId());
        List<Study> allStudies = studyRepository.findByPatientId(patient.getId());

        // ===== STATISTICS =====
        LocalDate lastVisit = allAppointments.stream()
                .filter(apt -> "completed".equals(apt.getStatus()))
                .map(Appointment::getAppointmentDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        detail.put("lastVisit", lastVisit);

        // Status
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        boolean isActive = lastVisit != null && lastVisit.isAfter(threeMonthsAgo);
        detail.put("status", isActive ? "active" : "inactive");

        detail.put("appointmentCount", allAppointments.size());
        detail.put("reportCount", allReports.size());
        detail.put("studyCount", allStudies.size());

        // ===== ALL APPOINTMENTS (for Appointments Tab) =====
        List<Map<String, Object>> appointmentsList = allAppointments.stream()
                .map(apt -> {
                    Map<String, Object> aptData = new HashMap<>();
                    aptData.put("id", apt.getId());
                    aptData.put("appointmentDate", apt.getAppointmentDate());
                    aptData.put("appointmentTime", apt.getAppointmentTime());
                    aptData.put("appointmentType", apt.getAppointmentType());
                    aptData.put("status", apt.getStatus());
                    aptData.put("location", apt.getLocation());
                    aptData.put("notes", apt.getNotes());
                    aptData.put("reason", apt.getReason());
                    aptData.put("durationMinutes", apt.getDurationMinutes());

                    // Doctor info
                    if (apt.getDoctor() != null) {
                        aptData.put("doctorName", apt.getDoctor().getFullName());
                        aptData.put("doctorId", apt.getDoctor().getId());
                    }

                    return aptData;
                })
                .collect(Collectors.toList());
        detail.put("recentAppointments", appointmentsList);

        // ===== ALL REPORTS (for Reports Tab) =====
        List<Map<String, Object>> reportsList = allReports.stream()
                .map(report -> {
                    Map<String, Object> reportData = new HashMap<>();
                    reportData.put("id", report.getId());
                    reportData.put("reportUid", report.getReportUid());
                    reportData.put("title", report.getTitle());
                    reportData.put("summary", report.getSummary());
                    reportData.put("findings", report.getFindings());
                    reportData.put("impression", report.getImpression());
                    reportData.put("recommendations", report.getRecommendations());
                    reportData.put("status", report.getStatus());
                    reportData.put("finalized", report.getFinalized());
                    reportData.put("createdAt", report.getCreatedAt());
                    reportData.put("finalizedAt", report.getFinalizedAt());

                    // Author info
                    if (report.getAuthor() != null) {
                        reportData.put("authorName", report.getAuthor().getFullName());
                        reportData.put("authorId", report.getAuthor().getId());
                    }

                    // Study info
                    if (report.getStudy() != null) {
                        reportData.put("studyId", report.getStudy().getId());
                        reportData.put("modality", report.getStudy().getModality());
                    }

                    return reportData;
                })
                .collect(Collectors.toList());
        detail.put("recentReports", reportsList);

        // ===== ALL STUDIES (for Studies Tab) =====
        List<Map<String, Object>> studiesList = allStudies.stream()
                .map(study -> {
                    Map<String, Object> studyData = new HashMap<>();
                    studyData.put("id", study.getId());
                    studyData.put("studyUid", study.getStudyUid());
                    studyData.put("studyDate", study.getStudyDate());
                    studyData.put("description", study.getDescription());
                    studyData.put("modality", study.getModality());
                    studyData.put("accessionNumber", study.getAccessionNumber());
                    studyData.put("status", study.getStatus());

                    // Count series
                    if (study.getSeriesList() != null) {
                        studyData.put("seriesCount", study.getSeriesList().size());
                    }

                    return studyData;
                })
                .collect(Collectors.toList());
        detail.put("recentStudies", studiesList);

        // Log audit
        auditService.log(
                "GET_PATIENT_COMPLETE_DETAIL",
                "PATIENT",
                id.toString(),
                doctor.getId(),
                doctor.getUsername()
        );

        return ResponseEntity.ok(detail);
    }

    /**
     * Create a new patient (called by doctors)
     * Optionally creates a User account for the patient
     */
    @PostMapping("/doctor/create")
    public ResponseEntity<?> createPatient(@RequestBody Map<String, Object> request) {
        User doctor = securityUtils.getCurrentUserOrThrow();

        // Verify doctor role
        if (!"DOCTOR".equals(doctor.getPrimaryRole()) && !securityUtils.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Only doctors can create patients"));
        }

        try {
            // Create Patient record
            Patient patient = new Patient();

            // Required fields
            String name = (String) request.get("name");
            String birthDateStr = (String) request.get("birthDate");
            String sex = (String) request.get("sex");
            String email = (String) request.get("email");

            if (name == null || birthDateStr == null || sex == null || email == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Name, birth date, gender, and email are required"));
            }

            // Check if email already exists
            if (patientRepository.findByUser_Email(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "A patient with this email already exists"));
            }

            patient.setName(name);
            patient.setBirthDate(LocalDate.parse(birthDateStr));
            patient.setSex(sex);
            patient.setEmail(email);

            // Optional fields
            if (request.containsKey("phone")) patient.setPhone((String) request.get("phone"));
            if (request.containsKey("bloodType")) patient.setBloodType((String) request.get("bloodType"));
            if (request.containsKey("address")) patient.setAddress((String) request.get("address"));
            if (request.containsKey("city")) patient.setCity((String) request.get("city"));
            if (request.containsKey("state")) patient.setState((String) request.get("state"));
            if (request.containsKey("zipCode")) patient.setZipCode((String) request.get("zipCode"));
            if (request.containsKey("emergencyContactName"))
                patient.setEmergencyContactName((String) request.get("emergencyContactName"));
            if (request.containsKey("emergencyContactPhone"))
                patient.setEmergencyContactPhone((String) request.get("emergencyContactPhone"));
            if (request.containsKey("emergencyContactRelationship"))
                patient.setEmergencyContactRelationship((String) request.get("emergencyContactRelationship"));
            if (request.containsKey("allergies")) patient.setAllergies((String) request.get("allergies"));
            if (request.containsKey("medicalConditions"))
                patient.setMedicalConditions((String) request.get("medicalConditions"));
            if (request.containsKey("currentMedications"))
                patient.setCurrentMedications((String) request.get("currentMedications"));

            patient.setPatientId("TEMP-" + email);  // ✅ Set temporary ID first

            Patient savedPatient = patient;
            Map<String, String> credentials = null;

            // Check if should create User account
            Boolean createUserAccount = (Boolean) request.get("createUserAccount");
            if (createUserAccount != null && createUserAccount) {
                // Check if user with email already exists
                if (userRepository.existsByEmail(email)) {
                    // User exists - just link the patient to existing user
                    User existingUser = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    patient.setUser(existingUser);
                } else {
                    // Create new User account
                    User user = new User();

                    // Generate username from email (part before @)
                    String username = email.split("@")[0];

                    // Make sure username is unique
                    String baseUsername = username;
                    int suffix = 1;
                    while (userRepository.existsByUsername(username)) {
                        username = baseUsername + suffix;
                        suffix++;
                    }

                    // Generate temporary password
                    String tempPassword = "Patient@" + (int)(Math.random() * 10000);

                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(tempPassword));
                    user.setEmail(email);
                    user.setFullName(name);
                    user.setPhone((String) request.get("phone"));
                    user.setBirthDate(LocalDate.parse(birthDateStr));
                    user.setEnabled(true);
                    user.setAccountLocked(false);

                    // Assign PATIENT role
                    Role patientRole = roleRepository.findByName("PATIENT")
                            .orElseGet(() -> {
                                Role newRole = new Role();
                                newRole.setName("PATIENT");
                                return roleRepository.save(newRole);
                            });

                    Set<Role> roles = new HashSet<>();
                    roles.add(patientRole);
                    user.setRoles(roles);

                    User savedUser = userRepository.save(user);
                    patient.setUser(savedUser);

                    // Return credentials
                    credentials = new HashMap<>();
                    credentials.put("username", username);
                    credentials.put("password", tempPassword);

                    auditService.log(AuditService.ACTION_CREATE, "USER", savedUser.getId().toString(),
                            doctor.getId(), doctor.getUsername());
                }
            }

            // Generate patient ID
            //  Save patient ONCE with temporary ID
            savedPatient = patientRepository.save(patient);
            patient.setPatientId("P-" + savedPatient.getId());
            savedPatient = patientRepository.save(patient);

            auditService.log(AuditService.ACTION_CREATE, "PATIENT", savedPatient.getId().toString(),
                    doctor.getId(), doctor.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Patient created successfully");
            response.put("patient", buildPatientCardData(savedPatient, doctor.getId()));
            if (credentials != null) {
                response.put("credentials", credentials);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private LocalDate getLastVisitDate(Long patientId, List<Appointment> appointments) {
        return appointments.stream()
                .filter(apt -> apt.getPatient().getId().equals(patientId))
                .filter(apt -> "completed".equals(apt.getStatus()))
                .map(Appointment::getAppointmentDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private Map<String, Object> buildPatientCardData(Patient patient, Long doctorId) {
        Map<String, Object> data = new HashMap<>();

        // Basic info
        data.put("id", patient.getId());
        data.put("patientId", patient.getPatientId());
        data.put("name", patient.getName());
        data.put("email", patient.getEmail());
        data.put("sex", patient.getSex());
        data.put("birthDate", patient.getBirthDate());

        // Calculate age
        if (patient.getBirthDate() != null) {
            int age = Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
            data.put("age", age);
        } else {
            data.put("age", null);
        }

        // Get appointments for this patient with this doctor
        List<Appointment> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId())
                .stream()
                .filter(apt -> apt.getDoctor() != null && apt.getDoctor().getId().equals(doctorId))
                .collect(Collectors.toList());

        // Last visit date
        LocalDate lastVisit = appointments.stream()
                .filter(apt -> "completed".equals(apt.getStatus()))
                .map(Appointment::getAppointmentDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        data.put("lastVisit", lastVisit);

        // Next appointment
        LocalDate nextAppointment = appointments.stream()
                .filter(apt -> "scheduled".equals(apt.getStatus()) || "confirmed".equals(apt.getStatus()))
                .map(Appointment::getAppointmentDate)
                .filter(date -> !date.isBefore(LocalDate.now()))
                .min(LocalDate::compareTo)
                .orElse(null);
        data.put("nextAppointment", nextAppointment);

        // Count stats
        long totalAppointments = appointments.size();
        long totalReports = reportRepository.findByPatientId(patient.getId()).size();
        long totalStudies = studyRepository.findByPatientId(patient.getId()).size();

        data.put("appointmentCount", totalAppointments);
        data.put("reportCount", totalReports);
        data.put("studyCount", totalStudies);

        // Status
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        boolean isActive = lastVisit != null && lastVisit.isAfter(threeMonthsAgo);
        data.put("status", isActive ? "active" : "inactive");

        // Flags (for future use - critical conditions, alerts, etc.)
        data.put("flags", new ArrayList<>());

        return data;
    }

    private Map<String, Object> buildPatientDetailData(Patient patient, Long doctorId) {
        Map<String, Object> data = buildPatientCardData(patient, doctorId);

        // Add detailed information
        List<Appointment> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());

        List<Report> reports = reportRepository.findByPatientId(patient.getId());
        List<Study> studies = studyRepository.findByPatientId(patient.getId());

        // Recent appointments (last 5)
        List<Map<String, Object>> recentAppointments = appointments.stream()
                .limit(5)
                .map(apt -> {
                    Map<String, Object> aptData = new HashMap<>();
                    aptData.put("id", apt.getId());
                    aptData.put("date", apt.getAppointmentDate());
                    aptData.put("time", apt.getAppointmentTime());
                    aptData.put("type", apt.getAppointmentType());
                    aptData.put("status", apt.getStatus());
                    return aptData;
                })
                .collect(Collectors.toList());
        data.put("recentAppointments", recentAppointments);

        // Recent reports (last 5)
        List<Map<String, Object>> recentReports = reports.stream()
                .limit(5)
                .map(report -> {
                    Map<String, Object> reportData = new HashMap<>();
                    reportData.put("id", report.getId());
                    reportData.put("title", report.getTitle());
                    reportData.put("date", report.getCreatedAt());
                    reportData.put("finalized", report.getFinalized());
                    return reportData;
                })
                .collect(Collectors.toList());
        data.put("recentReports", recentReports);

        // Recent studies (last 5)
        List<Map<String, Object>> recentStudies = studies.stream()
                .limit(5)
                .map(study -> {
                    Map<String, Object> studyData = new HashMap<>();
                    studyData.put("id", study.getId());
                    studyData.put("studyUid", study.getStudyUid());
                    studyData.put("modality", study.getModality());
                    studyData.put("date", study.getStudyDate());
                    return studyData;
                })
                .collect(Collectors.toList());
        data.put("recentStudies", recentStudies);

        return data;
    }
}