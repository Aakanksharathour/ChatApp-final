package com.chatapp.dto;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════
 *  MessageResponse  —  Shape of a message sent back to the client
 * ════════════════════════════════════════════════════════════════
 *
 *  Every message returned by the API looks like this JSON:
 *  {
 *    "messageId":  "64abc4560000000000000003",
 *    "chatId":     "64abc4560000000000000002",
 *    "senderId":   "64abc789...",
 *    "text":       "Hey, how are you?",
 *    "type":       "text",
 *    "fileUrl":    null,
 *    "timestamp":  "2026-06-09T10:30:00",
 *    "read":       false
 *  }
 *
 *  WHY RENAME id → messageId?
 *  ────────────────────────────
 *  The MongoDB field is "_id", Java field is "id".
 *  We rename it to "messageId" in the response so the frontend
 *  can easily tell messageId apart from chatId and senderId.
 *
 *  record = Java 16+ feature. Immutable data container.
 *  No need to write getters/setters/constructor manually.
 */
public record MessageResponse(
        String messageId,
        String chatId,
        String senderId,
        String text,
        String type,
        String fileUrl,
        LocalDateTime timestamp,
        boolean read
) {}
