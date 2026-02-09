package com.team.medaibackend.web;

import com.team.medaibackend.entity.*;
import com.team.medaibackend.repository.*;
import com.team.medaibackend.security.SecurityUtils;
import com.team.medaibackend.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final SecurityUtils securityUtils;

    public MessageController(
            MessageRepository messageRepository,
            MessageService messageService,
            SecurityUtils securityUtils) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.securityUtils = securityUtils;
    }

    /**
     * Send text message
     */
    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            User sender = securityUtils.getCurrentUserOrThrow();

            if (request.get("conversationId") == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Conversation ID is required"));
            }

            if (request.get("content") == null || request.get("content").toString().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Message content is required"));
            }

            Long conversationId = Long.valueOf(request.get("conversationId").toString());
            String content = request.get("content").toString();
            String senderType = "doctor"; // For now, assume doctor

            Message message = messageService.sendMessage(conversationId, sender, content, senderType);

            return ResponseEntity.ok(buildMessageDto(message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Send file message
     */
    @PostMapping("/upload")
    public ResponseEntity<?> sendFileMessage(
            @RequestParam("conversationId") Long conversationId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            User sender = securityUtils.getCurrentUserOrThrow();

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("message", "File size cannot exceed 10MB"));
            }

            String senderType = "doctor";
            Message message = messageService.sendFileMessage(conversationId, sender, file, senderType);

            return ResponseEntity.ok(buildMessageDto(message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Mark message as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            Message message = messageService.markAsRead(id);
            return ResponseEntity.ok(buildMessageDto(message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Mark all messages in conversation as read
     */
    @PutMapping("/conversation/{conversationId}/read-all")
    public ResponseEntity<?> markConversationAsRead(@PathVariable Long conversationId) {
        try {
            messageService.markConversationAsRead(conversationId);
            return ResponseEntity.ok(Map.of("message", "All messages marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Delete message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        try {
            User user = securityUtils.getCurrentUserOrThrow();
            messageService.deleteMessage(id, user);
            return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Helper method
    private Map<String, Object> buildMessageDto(Message msg) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", msg.getId());
        dto.put("conversationId", msg.getConversation().getId());
        dto.put("senderType", msg.getSenderType());
        dto.put("messageType", msg.getMessageType());
        dto.put("content", msg.getContent());
        dto.put("fileName", msg.getFileName());
        dto.put("filePath", msg.getFilePath());
        dto.put("fileType", msg.getFileType());
        dto.put("fileSize", msg.getFileSize());
        dto.put("isRead", msg.getIsRead());
        dto.put("readAt", msg.getReadAt());
        dto.put("sentAt", msg.getSentAt());

        if (msg.getSenderUser() != null) {
            dto.put("senderUserId", msg.getSenderUser().getId());
            dto.put("senderUserName", msg.getSenderUser().getFullName());
        }

        return dto;
    }
}