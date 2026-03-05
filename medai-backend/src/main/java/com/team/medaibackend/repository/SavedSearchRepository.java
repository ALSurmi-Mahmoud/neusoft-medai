package com.team.medaibackend.repository;

import com.team.medaibackend.entity.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    /**
     * Find all saved searches for a user
     */
    List<SavedSearch> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find public saved searches
     */
    List<SavedSearch> findByIsPublicTrueOrderByExecutionCountDesc();

    /**
     * Find quick filters
     */
    List<SavedSearch> findByIsQuickFilterTrueOrderByNameAsc();

    /**
     * Find quick filters by entity type
     */
    List<SavedSearch> findByIsQuickFilterTrueAndEntityTypeOrderByNameAsc(String entityType);

    /**
     * Find user's saved searches for specific entity type
     */
    @Query("SELECT s FROM SavedSearch s WHERE s.createdBy.id = :userId " +
            "AND s.entityType = :entityType ORDER BY s.updatedAt DESC")
    List<SavedSearch> findByUserAndEntityType(
            @Param("userId") Long userId,
            @Param("entityType") String entityType
    );

    /**
     * Find accessible saved searches (user's own + public)
     */
    @Query("SELECT s FROM SavedSearch s WHERE s.createdBy.id = :userId " +
            "OR s.isPublic = true ORDER BY s.updatedAt DESC")
    List<SavedSearch> findAccessibleSearches(@Param("userId") Long userId);

    /**
     * Check if name exists for user
     */
    boolean existsByNameAndCreatedById(String name, Long createdById);

    /**
     * Find by quick filter type
     */
    SavedSearch findByQuickFilterType(String quickFilterType);

    /**
     * Find most used searches
     */
    @Query("SELECT s FROM SavedSearch s WHERE s.isPublic = true " +
            "ORDER BY s.executionCount DESC")
    List<SavedSearch> findMostUsed();

    /**
     * Find recently used by user
     */
    @Query("SELECT s FROM SavedSearch s WHERE s.createdBy.id = :userId " +
            "AND s.lastExecutedAt IS NOT NULL ORDER BY s.lastExecutedAt DESC")
    List<SavedSearch> findRecentlyUsed(@Param("userId") Long userId);
}