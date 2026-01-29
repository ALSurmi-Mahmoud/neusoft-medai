package com.team.medaibackend.repository;

import com.team.medaibackend.entity.TreatmentPlanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TreatmentPlanTemplateRepository extends JpaRepository<TreatmentPlanTemplate, Long> {

    // Find system templates
    List<TreatmentPlanTemplate> findByIsSystemTrueOrderByUsageCountDesc();

    // Find by category
    List<TreatmentPlanTemplate> findByCategoryOrderByUsageCountDesc(String category);

    // Find by specialty
    List<TreatmentPlanTemplate> findBySpecialtyOrderByUsageCountDesc(String specialty);

    // Find user templates
    @Query("SELECT t FROM TreatmentPlanTemplate t WHERE t.isSystem = false ORDER BY t.createdAt DESC")
    List<TreatmentPlanTemplate> findUserTemplates();

    // Find popular templates
    @Query("SELECT t FROM TreatmentPlanTemplate t ORDER BY t.usageCount DESC")
    List<TreatmentPlanTemplate> findPopularTemplates();

    // Search templates by name
    @Query("SELECT t FROM TreatmentPlanTemplate t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY t.usageCount DESC")
    List<TreatmentPlanTemplate> searchTemplates(@Param("query") String query);
}