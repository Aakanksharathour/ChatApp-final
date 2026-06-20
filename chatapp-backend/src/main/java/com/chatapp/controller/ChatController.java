package com.chatapp.controller;

import com.chatapp.dto.ChatResponse;
import com.chatapp.dto.CreateChatRequest;
import com.chatapp.dto.SearchResult;
import com.chatapp.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════
 *  ChatController  —  Handles all Chat API endpoints
 * ════════════════════════════════════════════════════════════════
 *
 *  Endpoints:
 *  GET  /api/chats            → get all conversations
 *  POST /api/chats            → start a new conversation
 *  GET  /api/chats/:chatId    → get one specific conversation
 *
 *  All three require JWT token in Authorization header.
 *
 *  REMINDER — How Authentication works here:
 *  ───────────────────────────────────────────
 *  1. Request comes in with "Authorization: Bearer eyJ..."
 *  2. JwtAuthFilter reads the token, extracts userId, puts in SecurityContext
 *  3. Spring injects Authentication object into this controller method
 *  4. authentication.getName() gives us the userId
 *  5. We pass it to the service as "requesterId"
 *
 *  This is how every protected API knows WHO is making the request
 *  WITHOUT the user sending their ID in the request body.
 */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ════════════════════════════════════════════════════════════
    //  GET /api/chats/search?q=  —  Search chats
    // ════════════════════════════════════════════════════════════

    /**
     * SEARCH CONVERSATIONS
     *
     * URL:  GET http://localhost:8080/api/chats/search?q=alice
     *       GET http://localhost:8080/api/chats/search?q=hello&limit=10
     * Auth: Required
     *
     * Searches by contact name OR last message text (case-insensitive).
     * Minimum 2 characters required.
     *
     * SUCCESS RESPONSE (200 OK):
     * {
     *   "results": [
     *     {
     *       "chatId":              "64abc456...",
     *       "contactName":         "Alice Johnson",
     *       "contactProfilePhoto": null,
     *       "matchedMessage":      "Hey, how are you?",
     *       "timestamp":           "2026-05-31T10:30:00"
     *     }
     *   ]
     * }
     *
     * NOTE: Returns empty array [] when nothing matches — NOT a 404.
     * This follows the API doc spec exactly.
     *
     * WHY @GetMapping("/search") doesn't conflict with @GetMapping("/{chatId}"):
     * ─────────────────────────────────────────────────────────────────────────
     * Spring MVC gives priority to LITERAL paths over TEMPLATE paths.
     * So /api/chats/search is matched as the literal "/search" endpoint,
     * NOT as /{chatId} = "search". Spring is smart enough to figure this out.
     *
     * @param q     the search keyword (required, minimum 2 chars)
     * @param limit max results (optional, default 20)
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchChats(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        String requesterId = authentication.getName();

        List<SearchResult> results = chatService.searchChats(requesterId, q, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);

        return ResponseEntity.ok(response);   // 200 OK (even if results is empty)
    }

    // ════════════════════════════════════════════════════════════
    //  GET /api/chats  —  Get all chats
    // ════════════════════════════════════════════════════════════

    /**
     * GET ALL CONVERSATIONS
     *
     * URL:  GET http://localhost:8080/api/chats
     * Auth: Required (JWT token in Authorization header)
     *
     * SUCCESS RESPONSE (200 OK):
     * {
     *   "chats": [
     *     {
     *       "chatId":              "64abc456...",
     *       "contactId":           "64abc789...",
     *       "contactName":         "Alice Johnson",
     *       "contactProfilePhoto": null,
     *       "lastMessageText":     "Hey!",
     *       "lastMessageTime":     "2026-06-09T10:30:00",
     *       "unreadCount":         2,
     *       "updatedAt":           "2026-06-09T10:30:00"
     *     },
     *     ...
     *   ],
     *   "count": 3
     * }
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllChats(Authentication authentication) {
        // Get logged-in user's ID from JWT token
        String requesterId = authentication.getName();

        List<ChatResponse> chats = chatService.getAllChats(requesterId);

        Map<String, Object> response = new HashMap<>();
        response.put("chats", chats);
        response.put("count", chats.size());

        return ResponseEntity.ok(response);   // 200 OK
    }

    // ════════════════════════════════════════════════════════════
    //  POST /api/chats  —  Create new chat
    // ════════════════════════════════════════════════════════════

    /**
     * START A NEW CONVERSATION
     *
     * URL:  POST http://localhost:8080/api/chats
     * Auth: Required
     *
     * REQUEST BODY (JSON):
     * {
     *   "participantId": "64abc789..."
     * }
     *
     * SUCCESS RESPONSE (201 Created):
     * {
     *   "success": true,
     *   "chat": {
     *     "chatId":      "64abc456...",
     *     "contactName": "Alice Johnson",
     *     ...
     *   }
     * }
     *
     * If chat already exists → returns existing chat (still 201)
     * 400 → trying to chat with yourself
     * 404 → participantId doesn't exist
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createChat(
            @Valid @RequestBody CreateChatRequest request,
            Authentication authentication) {

        String requesterId = authentication.getName();

        ChatResponse chat = chatService.createChat(requesterId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("chat", chat);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);   // 201 Created
    }

    // ════════════════════════════════════════════════════════════
    //  GET /api/chats/:chatId  —  Get single chat
    // ════════════════════════════════════════════════════════════

    /**
     * GET ONE SPECIFIC CONVERSATION
     *
     * URL:  GET http://localhost:8080/api/chats/{chatId}
     * Auth: Required (and you must be a participant!)
     *
     * Example: GET http://localhost:8080/api/chats/64abc456...
     *
     * SUCCESS RESPONSE (200 OK):
     * {
     *   "chatId":              "64abc456...",
     *   "contactId":           "64abc789...",
     *   "contactName":         "Alice Johnson",
     *   "contactProfilePhoto": null,
     *   "lastMessageText":     "Hey!",
     *   "unreadCount":         0,
     *   ...
     * }
     *
     * 404 → chatId doesn't exist
     * 403 → you are not a participant of this chat
     *
     * @PathVariable String chatId  → reads {chatId} from the URL
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(
            @PathVariable String chatId,
            Authentication authentication) {

        String requesterId = authentication.getName();

        ChatResponse chat = chatService.getChatById(chatId, requesterId);

        return ResponseEntity.ok(chat);   // 200 OK
    }
}
