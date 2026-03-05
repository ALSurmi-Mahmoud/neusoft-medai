package com.team.medaibackend.repository;

import com.team.medaibackend.entity.SystemActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemActivityLogRepository extends JpaRepository<SystemActivityLog, Long> {

    Page<SystemActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<SystemActivityLog> findByActivityTypeOrderByCreatedAtDesc(String activityType);

    @Query("SELECT s FROM SystemActivityLog s WHERE s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<SystemActivityLog> findRecentActivities(@Param("since") LocalDateTime since);

    Page<SystemActivityLog> findByActivityTypeOrderByCreatedAtDesc(String activityType, Pageable pageable);
}