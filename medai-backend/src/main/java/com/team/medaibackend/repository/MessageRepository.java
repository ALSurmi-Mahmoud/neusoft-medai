package com.team.medaibackend.repository;

import com.team.medaibackend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find by conversation
    List<Message> findByConversationIdAndIsDeletedFalseOrderBySentAtAsc(Long conversationId);

    // Find recent messages
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.isDeleted = false AND m.sentAt >= :since ORDER BY m.sentAt ASC")
    List<Message> findRecentMessages(
            @Param("conversationId") Long conversationId,
            @Param("since") LocalDateTime since
    );

    // Find unread messages in conversation
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.isRead = false AND m.isDeleted = false ORDER BY m.sentAt ASC")
    List<Message> findUnreadMessages(@Param("conversationId") Long conversationId);

    // Count unread messages by sender type
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.isRead = false AND m.senderType = :senderType AND m.isDeleted = false")
    Long countUnreadBySenderType(
            @Param("conversationId") Long conversationId,
            @Param("senderType") String senderType
    );

    // Count unread messages for user (doctor)
    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.conversation.doctor.id = :userId " +
            "AND m.isRead = false AND m.senderType = 'patient' AND m.isDeleted = false")
    Long countUnreadByDoctor(@Param("userId") Long userId);

    // Find messages with attachments
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.messageType IN ('image', 'file') AND m.isDeleted = false " +
            "ORDER BY m.sentAt DESC")
    List<Message> findMessagesWithAttachments(@Param("conversationId") Long conversationId);

    // Search messages
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND m.isDeleted = false ORDER BY m.sentAt DESC")
    List<Message> searchMessages(
            @Param("conversationId") Long conversationId,
            @Param("query") String query
    );

    // Find latest message in conversation
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.isDeleted = false ORDER BY m.sentAt DESC")
    List<Message> findLatestMessage(@Param("conversationId") Long conversationId);

    // Delete old messages (cleanup)
    @Query("DELETE FROM Message m WHERE m.sentAt < :before AND m.isDeleted = true")
    void deleteOldDeletedMessages(@Param("before") LocalDateTime before);
}