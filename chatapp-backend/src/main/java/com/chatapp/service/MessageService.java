package com.chatapp.service;

import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.exception.ApiException;
import com.chatapp.model.Chat;
import com.chatapp.model.LastMessage;
import com.chatapp.model.Message;
import com.chatapp.repository.ChatRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ════════════════════════════════════════════════════════════════
 *  MessageService  —  Business logic for Messages APIs
 * ════════════════════════════════════════════════════════════════
 *
 *  Two operations:
 *  1. getMessages(chatId, requesterId, page, size)  → GET /api/chats/:chatId/messages
 *  2. sendMessage(chatId, requesterId, request)     → POST /api/chats/:chatId/messages
 *
 *  ── WHAT HAPPENS WHEN A MESSAGE IS SENT ─────────────────────────
 *
 *  Sending a message triggers 4 things in order:
 *
 *  STEP 1 — Save to DB
 *    Create a new Message document in the "messages" collection.
 *    This is the permanent record. Even if both users are offline,
 *    the message is safely stored.
 *
 *  STEP 2 — Update Chat's lastMessage cache
 *    The Chat document has a "lastMessage" embedded object that stores
 *    a COPY of the latest message. This avoids querying all messages
 *    just to show the chat list preview.
 *    After saving, we update this cache.
 *
 *  STEP 3 — Increment unread counts
 *    For every participant EXCEPT the sender, increment their unread count.
 *    Example: Alice sends a message → Bob's unreadCount++
 *    When Bob opens the chat → his count resets to 0.
 *
 *  STEP 4 — Push via WebSocket (real-time delivery)
 *    If the recipient is currently ONLINE (WebSocket connected),
 *    push the message to their browser INSTANTLY.
 *    If offline → they get it when they next call GET /messages.
 *
 *  ── READING MESSAGES ─────────────────────────────────────────────
 *
 *  When fetching messages, we also MARK THEM AS READ.
 *  This resets the unread count in the Chat document to 0.
 *  It's like WhatsApp's blue ticks — once you open a chat, all
 *  messages are considered read.
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ChatWebSocketHandler webSocketHandler;

    public MessageService(MessageRepository messageRepository,
                          ChatRepository chatRepository,
                          ChatWebSocketHandler webSocketHandler) {
        this.messageRepository = messageRepository;
        this.chatRepository    = chatRepository;
        this.webSocketHandler  = webSocketHandler;
    }

    // ════════════════════════════════════════════════════════════
    //  GET MESSAGES
    // ════════════════════════════════════════════════════════════

    /**
     * Returns message history for a chat, paginated.
     * Also marks the chat as read (resets unread count to 0).
     *
     * PAGINATION EXPLAINED:
     * ─────────────────────
     * We don't load ALL messages at once — that could be thousands!
     * Instead we load in "pages":
     *
     * page=0, size=50 → the 50 MOST RECENT messages
     * page=1, size=50 → the 50 messages BEFORE those
     * page=2, size=50 → the 50 messages BEFORE THOSE
     *
     * This is how WhatsApp/Messenger loads chat history:
     * - Open chat → see last 50 messages
     * - Scroll to top → loads the next 50 older ones
     *
     * We query in DESCENDING order (newest first) then REVERSE
     * the list so the response is in ascending order (oldest first),
     * which is how a chat UI displays messages.
     *
     * @param chatId       the chat to load messages for
     * @param requesterId  must be a participant (security check)
     * @param page         page number (0 = first page = most recent)
     * @param size         messages per page (default 50)
     */
    public List<MessageResponse> getMessages(String chatId, String requesterId, int page, int size) {
        // ── SECURITY: verify user is a chat participant ───────────────────
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException("Chat not found.", HttpStatus.NOT_FOUND));

        if (!chat.getParticipants().contains(requesterId)) {
            throw new ApiException("You are not a participant of this chat.", HttpStatus.FORBIDDEN);
        }

        log.debug("Fetching messages for chatId={} page={} size={}", chatId, page, size);

        // ── FETCH with pagination, newest first ──────────────────────────
        // PageRequest.of(page, size) = SQL: LIMIT size OFFSET page*size
        List<Message> messages = messageRepository.findByChatIdOrderByTimestampDesc(
                chatId,
                PageRequest.of(page, size)
        );

        // ── REVERSE to chronological order (oldest first at top) ─────────
        Collections.reverse(messages);

        // ── MARK AS READ: reset the requester's unread count to 0 ────────
        markChatAsRead(chat, requesterId);

        // ── MAP to response DTOs ─────────────────────────────────────────
        return messages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════
    //  SEND MESSAGE
    // ════════════════════════════════════════════════════════════

    /**
     * Saves a new message and delivers it in real-time via WebSocket.
     *
     * FLOW: validate → save to DB → update chat cache → push via WebSocket
     *
     * @param chatId       the conversation to send the message in
     * @param senderId     the logged-in user's ID (from JWT)
     * @param request      contains the message text
     */
    public MessageResponse sendMessage(String chatId, String senderId, SendMessageRequest request) {
        // ── SECURITY: verify sender is a participant ──────────────────────
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException("Chat not found.", HttpStatus.NOT_FOUND));

        if (!chat.getParticipants().contains(senderId)) {
            throw new ApiException("You are not a participant of this chat.", HttpStatus.FORBIDDEN);
        }

        // ── STEP 1: Save the message to MongoDB ───────────────────────────
        Message message = new Message(chatId, senderId, request.text());
        Message saved = messageRepository.save(message);
        log.info("Message saved: msgId={} chatId={} senderId={}", saved.getId(), chatId, senderId);

        // ── STEP 2: Update Chat's lastMessage cache ───────────────────────
        // Also updates the chat's updatedAt so it rises to the top of the list
        chat.setLastMessage(new LastMessage(saved.getText(), senderId, saved.getTimestamp()));
        chat.setUpdatedAt(saved.getTimestamp());

        // ── STEP 3: Increment unread count for ALL recipients ─────────────
        // The SENDER's count stays 0 (they obviously read their own message)
        if (chat.getUnreadCounts() == null) {
            chat.setUnreadCounts(new HashMap<>());
        }
        chat.getParticipants().stream()
                .filter(participantId -> !participantId.equals(senderId))
                .forEach(recipientId -> {
                    int current = chat.getUnreadCounts().getOrDefault(recipientId, 0);
                    chat.getUnreadCounts().put(recipientId, current + 1);
                });

        chatRepository.save(chat);

        // ── STEP 4: Push to online users via WebSocket ────────────────────
        // The "new_message" event instantly updates the recipient's UI
        MessageResponse response = toResponse(saved);
        pushNewMessageEvent(chat, chatId, response);

        return response;
    }

    // ════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    /**
     * Resets the logged-in user's unread count to 0 in the chat document.
     * Called when they load the message history (i.e., they've "opened" the chat).
     */
    private void markChatAsRead(Chat chat, String userId) {
        if (chat.getUnreadCounts() != null &&
                chat.getUnreadCounts().getOrDefault(userId, 0) > 0) {
            chat.getUnreadCounts().put(userId, 0);
            chatRepository.save(chat);
            log.debug("Marked chat {} as read for userId={}", chat.getId(), userId);
        }
    }

    /**
     * Sends a "new_message" WebSocket event to all chat participants who are online.
     *
     * WHY PUSH TO ALL PARTICIPANTS (including sender)?
     * ─────────────────────────────────────────────────
     * The sender might have the app open on multiple tabs/devices.
     * Pushing to the sender too ensures all their windows stay in sync.
     * The frontend can detect "senderId == myUserId" and render it
     * as a "sent" bubble instead of a "received" bubble.
     *
     * Format pushed to clients:
     * {
     *   "type":    "new_message",
     *   "chatId":  "...",
     *   "message": { messageId, chatId, senderId, text, type, timestamp, read }
     * }
     */
    private void pushNewMessageEvent(Chat chat, String chatId, MessageResponse message) {
        Map<String, Object> event = new HashMap<>();
        event.put("type",    "new_message");
        event.put("chatId",  chatId);
        event.put("message", message);

        chat.getParticipants().forEach(participantId ->
                webSocketHandler.sendToUser(participantId, event)
        );
    }

    /** Converts Message model → MessageResponse DTO */
    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChatId(),
                message.getSenderId(),
                message.getText(),
                message.getType(),
                message.getFileUrl(),
                message.getTimestamp(),
                message.isRead()
        );
    }
}
