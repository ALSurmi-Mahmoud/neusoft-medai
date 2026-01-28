package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // Find by prescription number
    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);

    // Find all prescriptions for a patient
    List<Prescription> findByPatientIdOrderByPrescribedDateDesc(Long patientId);

    // Find active prescriptions for a patient
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId " +
            "AND p.status = 'active' ORDER BY p.prescribedDate DESC")
    List<Prescription> findActiveByPatientId(@Param("patientId") Long patientId);

    // Find prescriptions by doctor
    List<Prescription> findByDoctorIdOrderByPrescribedDateDesc(Long doctorId);

    // Find prescriptions by status
    List<Prescription> findByStatus(String status);

    // Find prescriptions needing refill
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId " +
            "AND p.status = 'active' " +
            "AND p.refillsRemaining > 0 " +
            "AND p.endDate > CURRENT_DATE " +
            "ORDER BY p.endDate ASC")
    List<Prescription> findNeedingRefill(@Param("patientId") Long patientId);

    // Find expired prescriptions
    @Query("SELECT p FROM Prescription p WHERE p.endDate < CURRENT_DATE " +
            "AND p.status = 'active'")
    List<Prescription> findExpired();

    // Find prescriptions requiring follow-up
    @Query("SELECT p FROM Prescription p WHERE p.requiresFollowup = true " +
            "AND p.followupDate <= :date " +
            "AND p.status = 'active'")
    List<Prescription> findRequiringFollowup(@Param("date") LocalDate date);

    // Find by medication
    List<Prescription> findByMedicationIdAndStatus(Long medicationId, String status);

    // Count active prescriptions for patient
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.patient.id = :patientId " +
            "AND p.status = 'active'")
    Long countActiveByPatientId(@Param("patientId") Long patientId);

    // Find prescriptions for a patient within date range
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId " +
            "AND p.prescribedDate BETWEEN :startDate AND :endDate " +
            "ORDER BY p.prescribedDate DESC")
    List<Prescription> findByPatientAndDateRange(
            @Param("patientId") Long patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Check for existing active prescriptions of same medication
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId " +
            "AND (p.medication.id = :medicationId OR p.medicationName = :medicationName) " +
            "AND p.status = 'active'")
    List<Prescription> findActiveByPatientAndMedication(
            @Param("patientId") Long patientId,
            @Param("medicationId") Long medicationId,
            @Param("medicationName") String medicationName
    );
}