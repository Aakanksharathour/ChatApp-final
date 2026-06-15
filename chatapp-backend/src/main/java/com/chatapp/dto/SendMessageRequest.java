package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ════════════════════════════════════════════════════════════════
 *  SendMessageRequest  —  What the client sends when posting a message
 * ════════════════════════════════════════════════════════════════
 *
 *  This is the REQUEST BODY for POST /api/chats/:chatId/messages
 *
 *  CLIENT SENDS (JSON):
 *  {
 *    "text": "Hey, how are you?"
 *  }
 *
 *  NOTE: record = immutable class with auto-generated constructor,
 *        getters, equals, hashCode, and toString.
 *        Perfect for DTOs that are just data containers.
 *
 *  @NotBlank  → text cannot be null, empty, or just whitespace
 *  @Size      → max 5000 characters (prevents huge messages)
 */
public record SendMessageRequest(
        @NotBlank(message = "Message text cannot be blank.")
        @Size(max = 5000, message = "Message cannot exceed 5000 characters.")
        String text
) {}
