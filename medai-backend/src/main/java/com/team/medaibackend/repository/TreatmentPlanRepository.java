package com.team.medaibackend.repository;

import com.team.medaibackend.entity.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {

    // Find by UID
    Optional<TreatmentPlan> findByPlanUid(String planUid);

    // Find by patient
    List<TreatmentPlan> findByPatientIdOrderByStartDateDesc(Long patientId);

    // Find by patient and status
    List<TreatmentPlan> findByPatientIdAndStatusOrderByStartDateDesc(Long patientId, String status);

    // Find by doctor
    List<TreatmentPlan> findByDoctorIdOrderByStartDateDesc(Long doctorId);

    // Find active plans
    @Query("SELECT p FROM TreatmentPlan p WHERE p.status = 'active' ORDER BY p.startDate DESC")
    List<TreatmentPlan> findActivePlans();

    // Find active plans by patient
    @Query("SELECT p FROM TreatmentPlan p WHERE p.patient.id = :patientId AND p.status = 'active' " +
            "ORDER BY p.startDate DESC")
    List<TreatmentPlan> findActiveByPatient(@Param("patientId") Long patientId);

    // Find plans by priority
    List<TreatmentPlan> findByPriorityOrderByStartDateDesc(String priority);

    // Find plans by category
    List<TreatmentPlan> findByCategoryOrderByStartDateDesc(String category);

    // Find plans by date range
    @Query("SELECT p FROM TreatmentPlan p WHERE p.patient.id = :patientId " +
            "AND p.startDate BETWEEN :startDate AND :endDate ORDER BY p.startDate DESC")
    List<TreatmentPlan> findByPatientAndDateRange(
            @Param("patientId") Long patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find overdue plans
    @Query("SELECT p FROM TreatmentPlan p WHERE p.status = 'active' " +
            "AND p.expectedCompletionDate < :today ORDER BY p.expectedCompletionDate ASC")
    List<TreatmentPlan> findOverduePlans(@Param("today") LocalDate today);

    // Find plans needing attention (low progress, active)
    @Query("SELECT p FROM TreatmentPlan p WHERE p.status = 'active' " +
            "AND p.progressPercentage < :threshold ORDER BY p.progressPercentage ASC")
    List<TreatmentPlan> findLowProgressPlans(@Param("threshold") Integer threshold);

    // Count plans by status
    Long countByStatus(String status);

    // Count active plans by patient
    Long countByPatientIdAndStatus(Long patientId, String status);

    // Search plans
    @Query("SELECT p FROM TreatmentPlan p WHERE " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.diagnosis) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY p.startDate DESC")
    List<TreatmentPlan> searchPlans(@Param("query") String query);

    // Find plans by patient and doctor
    List<TreatmentPlan> findByPatientIdAndDoctorIdOrderByStartDateDesc(Long patientId, Long doctorId);

    // Get recent plans (last N days)
    @Query("SELECT p FROM TreatmentPlan p WHERE p.startDate >= :since ORDER BY p.startDate DESC")
    List<TreatmentPlan> findRecentPlans(@Param("since") LocalDate since);
}