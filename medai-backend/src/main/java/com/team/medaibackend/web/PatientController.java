package com.team.medaibackend.web;

import com.team.medaibackend.entity.Appointment;
import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.entity.Report;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.AppointmentRepository;
import com.team.medaibackend.repository.PatientRepository;
import com.team.medaibackend.repository.ReportRepository;
import com.team.medaibackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReportRepository reportRepository;

    public PatientController(UserRepository userRepository,
                             PatientRepository patientRepository,
                             AppointmentRepository appointmentRepository,
                             ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.reportRepository = reportRepository;
    }

    // Get patient's own profile
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Find patient record linked to this user (by email or username)
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

    // Get patient's own appointments
    @GetMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> getMyAppointments(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Find patient linked to this user
        Patient patient = patientRepository.findByUser_Email(user.getEmail()).orElse(null);

        if (patient == null) {
            // No patient record - return empty
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

    // Get patient's own reports
    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> getMyReports(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Find patient linked to this user
        Patient patient = patientRepository.findByUser_Email(user.getEmail()).orElse(null);

        if (patient == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Get reports for studies belonging to this patient
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
}