package com.chatapp.dto;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════
 *  ChatResponse  —  What we SEND BACK to the frontend for each chat
 * ════════════════════════════════════════════════════════════════
 *
 *  The frontend's Chat List Screen needs to show:
 *  - Contact's name and photo (from users collection)
 *  - Last message preview
 *  - How many unread messages
 *  - When the last message was sent
 *
 *  RAW CHAT DOCUMENT in MongoDB only has participant IDs, NOT names/photos.
 *  The ChatService joins the chat data WITH the user data to build this response.
 *
 *  WHAT FRONTEND RECEIVES (JSON):
 *  {
 *    "chatId":               "64abc456...",
 *    "contactId":            "64abc789...",
 *    "contactName":          "Alice Johnson",
 *    "contactProfilePhoto":  "https://cdn.example.com/photo.jpg",
 *    "lastMessageText":      "Hey, how are you?",
 *    "lastMessageSenderId":  "64abc123...",
 *    "lastMessageTime":      "2026-06-09T10:30:00",
 *    "unreadCount":          3,
 *    "createdAt":            "2026-01-15T00:00:00",
 *    "updatedAt":            "2026-06-09T10:30:00"
 *  }
 *
 *  NOTE: contactId, contactName, contactProfilePhoto come from the
 *  User document of the OTHER participant (not the logged-in user).
 */
public record ChatResponse(

    /** The chat's MongoDB _id */
    String chatId,

    /** The OTHER person's userId (not the logged-in user) */
    String contactId,

    /** The OTHER person's name — fetched from users collection */
    String contactName,

    /** The OTHER person's profile photo URL */
    String contactProfilePhoto,

    /** Text of the most recent message in this chat (null if no messages yet) */
    String lastMessageText,

    /** Who sent the last message (userId) */
    String lastMessageSenderId,

    /** When the last message was sent */
    LocalDateTime lastMessageTime,

    /** How many messages the LOGGED-IN USER hasn't read yet */
    int unreadCount,

    /** When this conversation was first created */
    LocalDateTime createdAt,

    /** When the last message was sent — used to sort the chat list */
    LocalDateTime updatedAt

) {}
