package com.team.medaibackend.web;

import com.team.medaibackend.repository.AppointmentRepository;
import com.team.medaibackend.repository.PatientRepository;
import com.team.medaibackend.repository.StudyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/nurse")
public class NurseController {

    private final StudyRepository studyRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    public NurseController(StudyRepository studyRepository,
                           AppointmentRepository appointmentRepository,
                           PatientRepository patientRepository) {
        this.studyRepository = studyRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('NURSE', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();

        // Count today's uploads (studies created today)
        long todayUploads = studyRepository.countByCreatedAtDate(today);

        // Count today's appointments
        long todayAppointments = appointmentRepository.countByAppointmentDate(today);

        // Total patients
        long totalPatients = patientRepository.count();

        // Total studies
        long totalStudies = studyRepository.count();

        stats.put("todayUploads", todayUploads);
        stats.put("todayAppointments", todayAppointments);
        stats.put("totalPatients", totalPatients);
        stats.put("totalStudies", totalStudies);

        return ResponseEntity.ok(stats);
    }
}