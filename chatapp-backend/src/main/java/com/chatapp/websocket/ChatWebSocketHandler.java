package com.chatapp.websocket;

import com.chatapp.repository.ChatRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ════════════════════════════════════════════════════════════════
 *  ChatWebSocketHandler  —  The heart of real-time chat
 * ════════════════════════════════════════════════════════════════
 *
 *  This class handles the WebSocket connection AFTER it's established.
 *  It's like the telephone operator — once you're connected, it manages
 *  what you say and routes it to the right person.
 *
 *  Extends TextWebSocketHandler — handles text-based WebSocket messages (JSON).
 *
 *  KEY DATA STRUCTURE:
 *  ─────────────────────────────────────────────────────────────
 *  sessions = Map<userId, WebSocketSession>
 *
 *  Example:
 *  {
 *    "alice123" → [Alice's open connection],
 *    "bob456"   → [Bob's open connection]
 *  }
 *
 *  When Alice sends a message to Bob:
 *  1. We look up Bob's session in this map
 *  2. We push the message directly to Bob's open connection
 *  3. Bob receives it INSTANTLY without polling
 *
 *  ConcurrentHashMap = thread-safe version of HashMap.
 *  Multiple users connect/disconnect at the same time, so we need thread safety.
 *
 *  FLOW DIAGRAM:
 *  ─────────────────────────────────────────────────────────────
 *
 *  Alice opens app           →  afterConnectionEstablished()
 *                               sessions["alice123"] = aliceSession
 *                               broadcasts "alice is online" to everyone
 *
 *  Alice types "Hey!"        →  handleTextMessage()
 *                               parses JSON → { type: "message", chatId: "c001", text: "Hey!" }
 *                               looks up chat c001 participants → [alice123, bob456]
 *                               sends to alice AND bob via sendToUser()
 *
 *  Alice types in text box   →  handleTextMessage()
 *                               parses JSON → { type: "typing", chatId: "c001" }
 *                               sends typing indicator to bob only (not alice herself)
 *
 *  Alice closes app          →  afterConnectionClosed()
 *                               sessions.remove("alice123")
 *                               broadcasts "alice is offline" to everyone
 *
 *  MESSAGE TYPES HANDLED:
 *  ─────────────────────────────────────────────────────────────
 *  From client → server:
 *    { "type": "message", "chatId": "c001", "text": "Hey!" }
 *    { "type": "typing",  "chatId": "c001" }
 *
 *  From server → client:
 *    { "type": "new_message", "chatId": "c001", "message": { msgId, senderId, text, timestamp } }
 *    { "type": "typing",      "chatId": "c001", "senderId": "alice123" }
 *    { "type": "status",      "userId": "alice123", "online": true/false }
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // Map of userId → their open WebSocket session
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ChatRepository chatRepository;

    // Jackson ObjectMapper — converts Java objects ↔ JSON strings
    // We configure it to handle LocalDateTime (Java 8 date type) properly
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ════════════════════════════════════════════════════════════
    //  CONNECTION OPENED
    // ════════════════════════════════════════════════════════════

    /**
     * Called when a user successfully connects via WebSocket.
     * We store their session and tell everyone they're online.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        sessions.put(userId, session);
        log.info("WebSocket CONNECTED: userId={}, total connected={}", userId, sessions.size());

        // Tell all OTHER connected users that this person is now online
        broadcastStatus(userId, true);
    }

    // ════════════════════════════════════════════════════════════
    //  MESSAGE RECEIVED
    // ════════════════════════════════════════════════════════════

    /**
     * Called when a client sends a message over the WebSocket.
     *
     * The message is always JSON text. We parse it and decide what to do
     * based on the "type" field.
     *
     * @param session      the sender's WebSocket session
     * @param textMessage  the raw JSON string sent by the client
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String senderId = getUserId(session);
        String rawJson = textMessage.getPayload();

        log.debug("WebSocket message from userId={}: {}", senderId, rawJson);

        // Parse the JSON string into a Java Map
        // TypeReference<Map<String, Object>> tells Jackson the exact type we expect
        Map<String, Object> payload = objectMapper.readValue(
                rawJson,
                new TypeReference<Map<String, Object>>() {}
        );

        String type   = (String) payload.get("type");
        String chatId = (String) payload.get("chatId");

        if (chatId == null || chatId.isBlank()) {
            log.warn("WebSocket message missing chatId from userId={}", senderId);
            return;
        }

        switch (type != null ? type : "") {
            case "message" -> handleChatMessage(senderId, chatId, (String) payload.get("text"));
            case "typing"  -> handleTyping(senderId, chatId);
            default        -> log.warn("Unknown WebSocket message type='{}' from userId={}", type, senderId);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  CONNECTION CLOSED
    // ════════════════════════════════════════════════════════════

    /**
     * Called when a user disconnects (closes browser, loses internet, etc.)
     * We remove their session and tell everyone they're offline.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserId(session);
        sessions.remove(userId);
        log.info("WebSocket DISCONNECTED: userId={}, reason={}, total connected={}", userId, status, sessions.size());

        // Tell all OTHER connected users this person went offline
        broadcastStatus(userId, false);
    }

    // ════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    /**
     * Handles a chat message from a user.
     * Broadcasts it to ALL participants of the chat (including the sender,
     * so their message appears instantly in their own UI too).
     */
    private void handleChatMessage(String senderId, String chatId, String text) {
        if (text == null || text.isBlank()) return;

        // Build the message object that we'll send to clients
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("msgId",     UUID.randomUUID().toString());
        messageData.put("senderId",  senderId);
        messageData.put("text",      text);
        messageData.put("timestamp", LocalDateTime.now().toString());
        messageData.put("type",      "text");

        // Wrap it in a "new_message" event
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type",    "new_message");
        broadcast.put("chatId",  chatId);
        broadcast.put("message", messageData);

        // Look up chat participants and send to each one
        chatRepository.findById(chatId).ifPresentOrElse(
                chat -> chat.getParticipants().forEach(participantId -> sendToUser(participantId, broadcast)),
                ()   -> log.warn("WebSocket: chatId={} not found", chatId)
        );
    }

    /**
     * Broadcasts a typing indicator to the OTHER participant only.
     * (No need to tell the typist themselves that they're typing.)
     */
    private void handleTyping(String senderId, String chatId) {
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type",     "typing");
        broadcast.put("chatId",   chatId);
        broadcast.put("senderId", senderId);

        chatRepository.findById(chatId).ifPresent(chat ->
                chat.getParticipants().stream()
                        .filter(id -> !id.equals(senderId))  // everyone EXCEPT the sender
                        .forEach(participantId -> sendToUser(participantId, broadcast))
        );
    }

    /**
     * Sends an online/offline status update to ALL currently connected users
     * (except the user whose status changed — no need to tell yourself).
     */
    private void broadcastStatus(String userId, boolean online) {
        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("type",   "status");
        statusPayload.put("userId", userId);
        statusPayload.put("online", online);

        sessions.forEach((connectedUserId, connectedSession) -> {
            if (!connectedUserId.equals(userId)) {
                sendToUser(connectedUserId, statusPayload);
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  PUBLIC UTILITY METHODS (used by MessageService later)
    // ════════════════════════════════════════════════════════════

    /**
     * Sends a JSON payload to a specific user's WebSocket connection.
     * If the user is not connected, the message is silently dropped
     * (they'll get it via the REST API when they come back online).
     *
     * @param userId   the recipient's userId
     * @param payload  any Java object — will be serialized to JSON
     */
    public void sendToUser(String userId, Object payload) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Failed to send WebSocket message to userId={}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * Checks if a user currently has an open WebSocket connection.
     * Used by other services to show online/offline indicators.
     */
    public boolean isUserOnline(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /** Reads the userId stored by WebSocketAuthInterceptor during handshake */
    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
}
