package com.team.medaibackend.repository;

import com.team.medaibackend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(CAST(:userId AS long) IS NULL OR a.userId = :userId) AND " +
            "(CAST(:action AS string) IS NULL OR a.action = :action) AND " +
            "(CAST(:resourceType AS string) IS NULL OR a.resourceType = :resourceType) AND " +
            "(CAST(:dateFrom AS timestamp) IS NULL OR a.createdAt >= :dateFrom) AND " +
            "(CAST(:dateTo AS timestamp) IS NULL OR a.createdAt <= :dateTo)")
    Page<AuditLog> findByFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    @Modifying
    @Query("UPDATE AuditLog a SET a.userId = NULL WHERE a.userId = :userId")
    void nullifyUserReferences(@Param("userId") Long userId);



    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);
}