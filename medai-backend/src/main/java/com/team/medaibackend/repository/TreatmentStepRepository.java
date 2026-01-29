package com.team.medaibackend.repository;

import com.team.medaibackend.entity.TreatmentStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TreatmentStepRepository extends JpaRepository<TreatmentStep, Long> {

    // Find all steps for a plan (ordered)
    List<TreatmentStep> findByPlanIdOrderByStepOrderAsc(Long planId);

    // Find steps by status
    List<TreatmentStep> findByPlanIdAndStatusOrderByStepOrderAsc(Long planId, String status);

    // Find pending steps for a plan
    @Query("SELECT s FROM TreatmentStep s WHERE s.plan.id = :planId AND s.status = 'pending' " +
            "ORDER BY s.stepOrder ASC")
    List<TreatmentStep> findPendingSteps(@Param("planId") Long planId);

    // Find next step to complete
    @Query("SELECT s FROM TreatmentStep s WHERE s.plan.id = :planId " +
            "AND s.status IN ('pending', 'in_progress') ORDER BY s.stepOrder ASC")
    List<TreatmentStep> findNextSteps(@Param("planId") Long planId);

    // Find overdue steps
    @Query("SELECT s FROM TreatmentStep s WHERE s.status IN ('pending', 'in_progress') " +
            "AND s.dueDate < :today ORDER BY s.dueDate ASC")
    List<TreatmentStep> findOverdueSteps(@Param("today") LocalDate today);

    // Find steps assigned to user
    List<TreatmentStep> findByAssignedToIdOrderByDueDateAsc(Long userId);

    // Find steps assigned to user by status
    List<TreatmentStep> findByAssignedToIdAndStatusOrderByDueDateAsc(Long userId, String status);

    // Count steps by plan and status
    Long countByPlanIdAndStatus(Long planId, String status);

    // Count total steps for plan
    Long countByPlanId(Long planId);

    // Find steps due soon (within N days)
    @Query("SELECT s FROM TreatmentStep s WHERE s.plan.id = :planId " +
            "AND s.status IN ('pending', 'in_progress') " +
            "AND s.dueDate BETWEEN :startDate AND :endDate " +
            "ORDER BY s.dueDate ASC")
    List<TreatmentStep> findStepsDueSoon(
            @Param("planId") Long planId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Delete all steps for a plan
    void deleteByPlanId(Long planId);
}