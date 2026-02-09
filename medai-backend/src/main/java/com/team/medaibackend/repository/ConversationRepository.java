package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Find by UID
    Optional<Conversation> findByConversationUid(String conversationUid);

    // Find by doctor
    List<Conversation> findByDoctorIdOrderByLastMessageAtDesc(Long doctorId);

    // Find by patient - ✅ ONLY ONE (removed duplicate at line 59)
    List<Conversation> findByPatientIdOrderByLastMessageAtDesc(Long patientId);

    // Find specific conversation between doctor and patient
    Optional<Conversation> findByDoctorIdAndPatientId(Long doctorId, Long patientId);

    // Find active conversations
    @Query("SELECT c FROM Conversation c WHERE c.status = 'active' ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findActiveConversations();

    // Find active conversations by doctor
    @Query("SELECT c FROM Conversation c WHERE c.doctor.id = :doctorId AND c.status = 'active' " +
            "ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findActiveByDoctor(@Param("doctorId") Long doctorId);

    // Find active conversations by patient
    @Query("SELECT c FROM Conversation c WHERE c.patient.id = :patientId AND c.status = 'active' " +
            "ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findActiveByPatient(@Param("patientId") Long patientId);

    // Find conversations with unread messages
    @Query("SELECT DISTINCT c FROM Conversation c " +
            "JOIN Message m ON m.conversation.id = c.id " +
            "WHERE c.doctor.id = :userId AND m.isRead = false AND m.senderType = 'patient' " +
            "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findWithUnreadMessagesByDoctor(@Param("userId") Long userId);

    // Count active conversations
    Long countByStatus(String status);

    // Count by doctor
    Long countByDoctorId(Long doctorId);

    // Find recent conversations (last N days)
    @Query("SELECT c FROM Conversation c WHERE c.lastMessageAt >= :since " +
            "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findRecentConversations(@Param("since") LocalDateTime since);

    // Search conversations
    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.doctor.id = :userId OR c.patient.id IN " +
            "(SELECT p.id FROM Patient p WHERE p.id IN :patientIds)) AND " +
            "(LOWER(c.patient.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.doctor.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.lastMessagePreview) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY c.lastMessageAt DESC")
    List<Conversation> searchConversations(
            @Param("userId") Long userId,
            @Param("patientIds") List<Long> patientIds,
            @Param("query") String query
    );
}