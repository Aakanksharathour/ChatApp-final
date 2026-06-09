package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * ════════════════════════════════════════════════════════════════
 *  CreateChatRequest  —  What frontend sends to START a new chat
 * ════════════════════════════════════════════════════════════════
 *
 *  To start a conversation, the frontend only needs to tell us:
 *  "I want to chat with THIS person"
 *
 *  REQUEST BODY (JSON):
 *  POST /api/chats
 *  {
 *    "participantId": "6b2f35803b579574bff0cf23"
 *  }
 *
 *  The logged-in user's ID comes from the JWT token (via JwtAuthFilter).
 *  So participants = [loggedInUserId, participantId]
 *
 *  We don't need to send both IDs from frontend — only the OTHER person's ID.
 */
public record CreateChatRequest(

    /**
     * The MongoDB _id of the user you want to start a chat with.
     * @NotBlank → cannot be empty
     */
    @NotBlank(message = "Participant ID is required.")
    String participantId

) {}
