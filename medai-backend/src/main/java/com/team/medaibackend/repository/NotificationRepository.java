package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find by user
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find unread notifications
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUser(@Param("userId") Long userId);

    // Count unread notifications
    Long countByUserIdAndIsReadFalse(Long userId);

    // Find by type
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);

    // Find by priority
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.priority = :priority " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndPriority(
            @Param("userId") Long userId,
            @Param("priority") String priority
    );

    // Find recent notifications (last N days)
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // Mark all as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    // Delete expired notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    // Delete old read notifications (cleanup)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :before")
    void deleteOldRead(@Param("before") LocalDateTime before);

    // Find by related entity
    @Query("SELECT n FROM Notification n WHERE n.relatedEntityType = :entityType " +
            "AND n.relatedEntityId = :entityId ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId
    );
}