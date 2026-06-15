package com.chatapp.controller;

import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.service.MessageService;
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
 *  MessageController  —  Message history and sending APIs
 * ════════════════════════════════════════════════════════════════
 *
 *  Endpoints:
 *  GET  /api/chats/{chatId}/messages          → get message history
 *  POST /api/chats/{chatId}/messages          → send a new message
 *
 *  Both require JWT token in Authorization header.
 *
 *  URL STRUCTURE EXPLAINED:
 *  ────────────────────────
 *  /api/chats/{chatId}/messages
 *          ↑
 *          This is a path variable — the actual chatId goes here.
 *
 *  Example:
 *  GET http://localhost:8080/api/chats/64abc456.../messages
 *
 *  The {chatId} part is captured by @PathVariable in the method signature.
 *  Spring automatically extracts "64abc456..." and passes it to the method.
 */
@RestController
@RequestMapping("/api/chats/{chatId}/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // ════════════════════════════════════════════════════════════
    //  GET /api/chats/{chatId}/messages  —  Load message history
    // ════════════════════════════════════════════════════════════

    /**
     * GET MESSAGE HISTORY
     *
     * URL:  GET http://localhost:8080/api/chats/{chatId}/messages
     *       GET http://localhost:8080/api/chats/{chatId}/messages?page=1&size=50
     * Auth: Required (must be a participant of the chat)
     *
     * QUERY PARAMETERS:
     * - page (optional, default 0) → which page of messages (0 = most recent)
     * - size (optional, default 50) → how many messages per page
     *
     * SUCCESS RESPONSE (200 OK):
     * {
     *   "messages": [
     *     {
     *       "messageId":  "64abc4560000000000000003",
     *       "chatId":     "64abc4560000000000000002",
     *       "senderId":   "64abc789...",
     *       "text":       "Hey, how are you?",
     *       "type":       "text",
     *       "fileUrl":    null,
     *       "timestamp":  "2026-06-09T10:30:00",
     *       "read":       true
     *     },
     *     ...
     *   ],
     *   "page":  0,
     *   "size":  50,
     *   "count": 23
     * }
     *
     * Side effect: resets the caller's unread count to 0.
     *
     * ERROR RESPONSES:
     * 403 → you are not a participant of this chat
     * 404 → chatId doesn't exist
     *
     * @param chatId         from the URL path
     * @param page           page number (0-indexed, default 0)
     * @param size           messages per page (default 50)
     * @param authentication injected by Spring — gives us the logged-in userId
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        String requesterId = authentication.getName();

        List<MessageResponse> messages = messageService.getMessages(chatId, requesterId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messages);
        response.put("page",     page);
        response.put("size",     size);
        response.put("count",    messages.size());

        return ResponseEntity.ok(response);   // 200 OK
    }

    // ════════════════════════════════════════════════════════════
    //  POST /api/chats/{chatId}/messages  —  Send a message
    // ════════════════════════════════════════════════════════════

    /**
     * SEND A NEW MESSAGE
     *
     * URL:  POST http://localhost:8080/api/chats/{chatId}/messages
     * Auth: Required (must be a participant of the chat)
     *
     * REQUEST BODY (JSON):
     * {
     *   "text": "Hey, how are you?"
     * }
     *
     * SUCCESS RESPONSE (201 Created):
     * {
     *   "success": true,
     *   "message": {
     *     "messageId":  "64abc4560000000000000003",
     *     "chatId":     "64abc4560000000000000002",
     *     "senderId":   "64abc789...",
     *     "text":       "Hey, how are you?",
     *     "type":       "text",
     *     "fileUrl":    null,
     *     "timestamp":  "2026-06-09T10:30:00",
     *     "read":       false
     *   }
     * }
     *
     * What happens in the background:
     * 1. Message saved to "messages" collection in MongoDB
     * 2. Chat's lastMessage cache updated
     * 3. Recipient's unread count incremented
     * 4. If recipient is online → WebSocket push delivers it instantly
     *
     * ERROR RESPONSES:
     * 400 → text is blank or too long
     * 403 → you are not a participant of this chat
     * 404 → chatId doesn't exist
     *
     * @param chatId         from the URL path
     * @param request        the message body (validated by @Valid)
     * @param authentication gives us the logged-in userId
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable String chatId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {

        String senderId = authentication.getName();

        MessageResponse message = messageService.sendMessage(chatId, senderId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);   // 201 Created
    }
}
