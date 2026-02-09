package com.team.medaibackend.service;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    public NotificationService(
            NotificationRepository notificationRepository,
            EmailService emailService,
            AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    /**
     * Create message notification
     */
    @Transactional
    public Notification createMessageNotification(User recipient, Message message, String senderName) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType("message");
        notification.setTitle("New message from " + senderName);
        notification.setContent(truncate(message.getContent(), 100));
        notification.setRelatedEntityType("message");
        notification.setRelatedEntityId(message.getId());
        notification.setActionUrl("/messages/" + message.getConversation().getId());
        notification.setPriority("normal");

        Notification saved = notificationRepository.save(notification);

        // Send email notification async
        if (recipient.getEmail() != null) {
            sendEmailNotificationAsync(saved, recipient);
        }

        return saved;
    }

    /**
     * Create appointment notification
     */
    @Transactional
    public Notification createAppointmentNotification(
            User recipient,
            Long appointmentId,
            String title,
            String content
    ) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType("appointment");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedEntityType("appointment");
        notification.setRelatedEntityId(appointmentId);
        notification.setActionUrl("/appointments/" + appointmentId);
        notification.setPriority("high");

        return notificationRepository.save(notification);
    }

    /**
     * Create prescription notification
     */
    @Transactional
    public Notification createPrescriptionNotification(
            User recipient,
            Long prescriptionId,
            String medicationName
    ) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType("prescription");
        notification.setTitle("Prescription Ready");
        notification.setContent("Your prescription for " + medicationName + " is ready.");
        notification.setRelatedEntityType("prescription");
        notification.setRelatedEntityId(prescriptionId);
        notification.setActionUrl("/prescriptions/" + prescriptionId);
        notification.setPriority("normal");

        return notificationRepository.save(notification);
    }

    /**
     * Create treatment plan notification
     */
    @Transactional
    public Notification createTreatmentPlanNotification(
            User recipient,
            Long planId,
            String planTitle
    ) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType("treatment_plan");
        notification.setTitle("Treatment Plan Updated");
        notification.setContent("Your treatment plan \"" + planTitle + "\" has been updated.");
        notification.setRelatedEntityType("treatment_plan");
        notification.setRelatedEntityId(planId);
        notification.setActionUrl("/treatment-plans/" + planId);
        notification.setPriority("normal");

        return notificationRepository.save(notification);
    }

    /**
     * Create system notification
     */
    @Transactional
    public Notification createSystemNotification(User recipient, String title, String content) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType("system");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setPriority("normal");

        return notificationRepository.save(notification);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    /**
     * Get unread count
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Send email notification asynchronously
     */
    @Async
    protected void sendEmailNotificationAsync(Notification notification, User recipient) {
        try {
            emailService.sendNotificationEmail(
                    recipient.getEmail(),
                    notification.getTitle(),
                    notification.getContent()
            );

            // Update notification
            notification.setSentEmail(true);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
        } catch (Exception e) {
            // Log error but don't fail notification creation
            System.err.println("Failed to send email notification: " + e.getMessage());
        }
    }

    /**
     * Helper: Truncate string
     */
    private String truncate(String str, int length) {
        if (str == null) return "";
        return str.length() > length ? str.substring(0, length) + "..." : str;
    }
}