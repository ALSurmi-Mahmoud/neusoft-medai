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
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public ConversationController(
            ConversationRepository conversationRepository,
            ConversationService conversationService,
            MessageRepository messageRepository,
            SecurityUtils securityUtils,
            UserRepository userRepository,
            PatientRepository patientRepository) {
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Get all conversations for current user
     */
    @GetMapping
    public ResponseEntity<?> getAllConversations() {
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            List<Conversation> conversations;

            if (securityUtils.isDoctor() || securityUtils.isAdmin()) {
                // Doctors see conversations with their patients
                conversations = conversationRepository.findByDoctorIdOrderByLastMessageAtDesc(currentUser.getId());
            } else {
                // Patients see conversations with doctors
                // ✅ FIXED: Use findByUser_Id (accepts Long userId) instead of findByPatientId (accepts String patientId)
                Patient patient = patientRepository.findByUser_Id(currentUser.getId())
                        .orElseThrow(() -> new RuntimeException("Patient record not found"));
                conversations = conversationRepository.findByPatientIdOrderByLastMessageAtDesc(patient.getId());
            }

            List<Map<String, Object>> result = conversations.stream()
                    .map(this::buildConversationDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get or create conversation
     * - Doctors can create with patientId
     * - Patients can create with doctorId
     */
    @PostMapping
    public ResponseEntity<?> getOrCreateConversation(@RequestBody Map<String, Object> request) {
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();

            if (securityUtils.isDoctor() || securityUtils.isAdmin()) {
                // Doctor creating conversation with patient
                if (request.get("patientId") == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Patient ID is required"));
                }
                Long patientId = Long.valueOf(request.get("patientId").toString());
                Conversation conversation = conversationService.getOrCreateConversation(currentUser.getId(), patientId);
                return ResponseEntity.ok(buildConversationDto(conversation));

            } else {
                // Patient creating conversation with doctor
                if (request.get("doctorId") == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Doctor ID is required"));
                }
                Long doctorId = Long.valueOf(request.get("doctorId").toString());

                // ✅ FIXED: Use findByUser_Id (accepts Long userId) instead of findByPatientId (accepts String patientId)
                Patient patient = patientRepository.findByUser_Id(currentUser.getId())
                        .orElseThrow(() -> new RuntimeException("Patient record not found for user"));

                // Create conversation with doctor
                Conversation conversation = conversationService.getOrCreateConversation(doctorId, patient.getId());
                return ResponseEntity.ok(buildConversationDto(conversation));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get conversation details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getConversation(@PathVariable Long id) {
        try {
            Conversation conversation = conversationRepository.findById(id).orElse(null);
            if (conversation == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> result = buildConversationDto(conversation);
            result.put("stats", conversationService.getConversationStats(id));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get messages in conversation
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id) {
        try {
            List<Message> messages = messageRepository.findByConversationIdAndIsDeletedFalseOrderBySentAtAsc(id);

            List<Map<String, Object>> result = messages.stream()
                    .map(this::buildMessageDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Archive conversation
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveConversation(@PathVariable Long id) {
        try {
            User user = securityUtils.getCurrentUserOrThrow();
            Conversation archived = conversationService.archiveConversation(id, user);
            return ResponseEntity.ok(buildConversationDto(archived));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Toggle mute
     */
    @PutMapping("/{id}/mute")
    public ResponseEntity<?> toggleMute(@PathVariable Long id) {
        try {
            User user = securityUtils.getCurrentUserOrThrow();
            Conversation conversation = conversationService.toggleMute(id, user);
            return ResponseEntity.ok(buildConversationDto(conversation));
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
            User user = securityUtils.getCurrentUserOrThrow();
            Long count = conversationService.getUnreadCount(user.getId());
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> buildConversationDto(Conversation conv) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", conv.getId());
        dto.put("conversationUid", conv.getConversationUid());
        dto.put("status", conv.getStatus());
        dto.put("isMuted", conv.getIsMuted());
        dto.put("lastMessageAt", conv.getLastMessageAt());
        dto.put("lastMessagePreview", conv.getLastMessagePreview());
        dto.put("createdAt", conv.getCreatedAt());
        dto.put("updatedAt", conv.getUpdatedAt());

        if (conv.getDoctor() != null) {
            dto.put("doctorId", conv.getDoctor().getId());
            dto.put("doctorName", conv.getDoctor().getFullName());
        }

        if (conv.getPatient() != null) {
            dto.put("patientId", conv.getPatient().getId());
            dto.put("patientName", conv.getPatient().getName());
            dto.put("patientIdNumber", conv.getPatient().getPatientId());
        }

        // Add unread count
        Long unreadCount = messageRepository.countUnreadBySenderType(conv.getId(), "patient");
        dto.put("unreadCount", unreadCount);

        return dto;
    }

    private Map<String, Object> buildMessageDto(Message msg) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", msg.getId());
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