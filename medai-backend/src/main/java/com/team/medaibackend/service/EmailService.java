package com.team.medaibackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@medai.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send simple email
     */
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }

    /**
     * Send notification email
     */
    @Async
    public void sendNotificationEmail(String to, String title, String content) {
        String subject = "MedAI Notification: " + title;
        String body = String.format(
                "You have a new notification:\n\n%s\n\n%s\n\n" +
                        "Log in to MedAI to view details.",
                title,
                content
        );

        sendEmail(to, subject, body);
    }

    /**
     * Send appointment reminder email
     */
    @Async
    public void sendAppointmentReminder(
            String to,
            String patientName,
            String doctorName,
            String appointmentDate
    ) {
        String subject = "Appointment Reminder";
        String body = String.format(
                "Dear %s,\n\n" +
                        "This is a reminder that you have an appointment with Dr. %s on %s.\n\n" +
                        "Please arrive 15 minutes early.\n\n" +
                        "Best regards,\n" +
                        "MedAI Team",
                patientName,
                doctorName,
                appointmentDate
        );

        sendEmail(to, subject, body);
    }
}