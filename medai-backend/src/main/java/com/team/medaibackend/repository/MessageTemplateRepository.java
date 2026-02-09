package com.team.medaibackend.repository;

import com.team.medaibackend.entity.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    // Find system templates
    List<MessageTemplate> findByIsSystemTrueOrderByUsageCountDesc();

    // Find by category
    List<MessageTemplate> findByCategoryOrderByUsageCountDesc(String category);

    // Find user templates
    @Query("SELECT t FROM MessageTemplate t WHERE t.isSystem = false " +
            "AND t.createdBy.id = :userId ORDER BY t.createdAt DESC")
    List<MessageTemplate> findUserTemplates(@Param("userId") Long userId);

    // Find all accessible templates (system + user's)
    @Query("SELECT t FROM MessageTemplate t WHERE t.isSystem = true " +
            "OR t.createdBy.id = :userId ORDER BY t.usageCount DESC")
    List<MessageTemplate> findAccessibleTemplates(@Param("userId") Long userId);

    // Find popular templates
    @Query("SELECT t FROM MessageTemplate t ORDER BY t.usageCount DESC")
    List<MessageTemplate> findPopularTemplates();

    // Search templates
    @Query("SELECT t FROM MessageTemplate t WHERE " +
            "(t.isSystem = true OR t.createdBy.id = :userId) AND " +
            "(LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY t.usageCount DESC")
    List<MessageTemplate> searchTemplates(
            @Param("userId") Long userId,
            @Param("query") String query
    );

    // Check if template name exists
    boolean existsByNameAndCreatedById(String name, Long createdById);
}