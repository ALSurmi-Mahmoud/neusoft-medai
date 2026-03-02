package com.team.medaibackend.repository;

import com.team.medaibackend.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    // Find by category
    List<ReportTemplate> findByCategoryAndIsActiveTrueOrderByUsageCountDesc(String category);

    // Find system templates
    List<ReportTemplate> findByIsSystemTrueAndIsActiveTrueOrderByUsageCountDesc();

    // Find user templates
    @Query("SELECT t FROM ReportTemplate t WHERE t.createdBy.id = :userId " +
            "AND t.isActive = true ORDER BY t.usageCount DESC, t.createdAt DESC")
    List<ReportTemplate> findByUser(@Param("userId") Long userId);

    // Find all accessible templates (system + user's own)
    @Query("SELECT t FROM ReportTemplate t WHERE t.isActive = true AND " +
            "(t.isSystem = true OR t.createdBy.id = :userId) " +
            "ORDER BY t.isSystem DESC, t.usageCount DESC")
    List<ReportTemplate> findAccessibleTemplates(@Param("userId") Long userId);

    // Find by category (system + user's own)
    @Query("SELECT t FROM ReportTemplate t WHERE t.isActive = true AND " +
            "t.category = :category AND (t.isSystem = true OR t.createdBy.id = :userId) " +
            "ORDER BY t.isSystem DESC, t.usageCount DESC")
    List<ReportTemplate> findByCategoryForUser(
            @Param("category") String category,
            @Param("userId") Long userId
    );

    // Search templates
    @Query("SELECT t FROM ReportTemplate t WHERE t.isActive = true AND " +
            "(t.isSystem = true OR t.createdBy.id = :userId) AND " +
            "(LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY t.usageCount DESC")
    List<ReportTemplate> searchTemplates(
            @Param("query") String query,
            @Param("userId") Long userId
    );

    // Find most used templates
    @Query("SELECT t FROM ReportTemplate t WHERE t.isActive = true " +
            "ORDER BY t.usageCount DESC")
    List<ReportTemplate> findMostUsed();

    // Check if template name exists for user
    boolean existsByNameAndCreatedById(String name, Long createdById);
}