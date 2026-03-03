package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPatientId(String patientId);
    Optional<Patient> findByUser_Id(Long userId);

    // ✅ NEW: query through relation Patient.user.email
    Optional<Patient> findByUser_Email(String email);

    // Analytics methods
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Patient> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}