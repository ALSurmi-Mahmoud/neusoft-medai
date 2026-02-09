package com.team.medaibackend.web;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import com.team.medaibackend.security.SecurityUtils;
import com.team.medaibackend.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public NotificationController(
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    /**
     * Get all notifications for current user
     */
    @GetMapping
    public ResponseEntity<?> getAllNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly
    ) {
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            List<Notification> notifications;

            if (unreadOnly) {
                notifications = notificationRepository.findUnreadByUser(currentUser.getId());
            } else if (type != null) {
                notifications = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(currentUser.getId(), type);
            } else {
                notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
            }

            List<Map<String, Object>> result = notifications.stream()
                    .map(this::buildNotificationDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get unread count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            Long count = notificationService.getUnreadCount(currentUser.getId());
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(buildNotificationDto(notification));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Mark all as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            notificationService.markAllAsRead(currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Helper method
    private Map<String, Object> buildNotificationDto(Notification notif) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", notif.getId());
        dto.put("type", notif.getType());
        dto.put("title", notif.getTitle());
        dto.put("content", notif.getContent());
        dto.put("relatedEntityType", notif.getRelatedEntityType());
        dto.put("relatedEntityId", notif.getRelatedEntityId());
        dto.put("actionUrl", notif.getActionUrl());
        dto.put("isRead", notif.getIsRead());
        dto.put("readAt", notif.getReadAt());
        dto.put("priority", notif.getPriority());
        dto.put("createdAt", notif.getCreatedAt());
        return dto;
    }
}