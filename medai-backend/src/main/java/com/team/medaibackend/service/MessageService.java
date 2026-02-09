package com.team.medaibackend.service;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    private static final String UPLOAD_DIR = "storage/messages/";

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            NotificationService notificationService,
            AuditService auditService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    /**
     * Send text message
     */
    @Transactional
    public Message sendMessage(Long conversationId, User sender, String content, String senderType) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderUser(sender);
        message.setSenderType(senderType);
        message.setMessageType("text");
        message.setContent(content);

        Message saved = messageRepository.save(message);

        // Conversation will be auto-updated by database trigger

        // Create notification for recipient
        if ("doctor".equals(senderType)) {
            // Notify patient (no User object for patients, would need separate notification system)
            // For now, skip patient notifications
        } else if ("patient".equals(senderType)) {
            // Notify doctor
            notificationService.createMessageNotification(
                    conversation.getDoctor(),
                    saved,
                    conversation.getPatient().getName()
            );
        }

        // Audit log
        auditService.log(
                "MESSAGE_SENT",
                "MESSAGE",
                saved.getId().toString(),
                sender.getId(),
                sender.getUsername()
        );

        return saved;
    }

    /**
     * Upload and send file message
     */
    @Transactional
    public Message sendFileMessage(
            Long conversationId,
            User sender,
            MultipartFile file,
            String senderType
    ) throws IOException {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Save file
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // Determine message type
        String contentType = file.getContentType();
        String messageType = contentType != null && contentType.startsWith("image/") ? "image" : "file";

        // Create message
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderUser(sender);
        message.setSenderType(senderType);
        message.setMessageType(messageType);
        message.setContent(file.getOriginalFilename());
        message.setFileName(file.getOriginalFilename());
        message.setFilePath(filePath.toString());
        message.setFileType(contentType);
        message.setFileSize(file.getSize());

        Message saved = messageRepository.save(message);

        // Create notification
        if ("patient".equals(senderType)) {
            notificationService.createMessageNotification(
                    conversation.getDoctor(),
                    saved,
                    conversation.getPatient().getName()
            );
        }

        auditService.log(
                "FILE_MESSAGE_SENT",
                "MESSAGE",
                saved.getId().toString(),
                sender.getId(),
                sender.getUsername()
        );

        return saved;
    }

    /**
     * Mark message as read
     */
    @Transactional
    public Message markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            return messageRepository.save(message);
        }

        return message;
    }

    /**
     * Mark all messages in conversation as read
     */
    @Transactional
    public void markConversationAsRead(Long conversationId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessages(conversationId);

        for (Message message : unreadMessages) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
        }

        messageRepository.saveAll(unreadMessages);
    }

    /**
     * Delete message (soft delete)
     */
    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setIsDeleted(true);
        messageRepository.save(message);

        auditService.log(
                "MESSAGE_DELETED",
                "MESSAGE",
                messageId.toString(),
                user.getId(),
                user.getUsername()
        );
    }
}