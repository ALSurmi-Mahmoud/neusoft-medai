package com.team.medaibackend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map of userId -> WebSocketSession
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract userId from query params (you'll need to implement auth)
        String userId = session.getUri().getQuery(); // Simple example
        if (userId != null && userId.startsWith("userId=")) {
            Long id = Long.valueOf(userId.substring(7));
            sessions.put(id, session);
            System.out.println("WebSocket connection established for user: " + id);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Parse incoming message
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);

        String type = (String) payload.get("type");

        if ("message".equals(type)) {
            // Handle new message - broadcast to recipient
            Long recipientId = Long.valueOf(payload.get("recipientId").toString());
            WebSocketSession recipientSession = sessions.get(recipientId);

            if (recipientSession != null && recipientSession.isOpen()) {
                recipientSession.sendMessage(message);
            }
        } else if ("typing".equals(type)) {
            // Handle typing indicator
            Long recipientId = Long.valueOf(payload.get("recipientId").toString());
            WebSocketSession recipientSession = sessions.get(recipientId);

            if (recipientSession != null && recipientSession.isOpen()) {
                recipientSession.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Remove session
        sessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        System.out.println("WebSocket connection closed");
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(Long userId, Object message) throws IOException {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }
}