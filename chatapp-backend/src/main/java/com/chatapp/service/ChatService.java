package com.chatapp.service;

import com.chatapp.dto.ChatResponse;
import com.chatapp.dto.CreateChatRequest;
import com.chatapp.dto.SearchResult;
import com.chatapp.exception.ApiException;
import com.chatapp.model.Chat;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRepository;
import com.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

/**
 * ════════════════════════════════════════════════════════════════
 *  ChatService  —  BUSINESS LOGIC for Chat APIs
 * ════════════════════════════════════════════════════════════════
 *
 *  Three operations:
 *  1. getAllChats(requesterId)              → GET /api/chats
 *  2. createChat(requesterId, request)     → POST /api/chats
 *  3. getChatById(chatId, requesterId)     → GET /api/chats/:chatId
 *
 *  KEY CONCEPT: JOINING TWO COLLECTIONS
 *  ─────────────────────────────────────
 *  MongoDB is NOT a relational database — it has no automatic JOIN.
 *  But we need data from BOTH chats AND users collections.
 *
 *  Example: Chat document has participants: ["userId1", "userId2"]
 *  But frontend needs contactName and contactProfilePhoto.
 *  Those are in the USERS collection, not chats!
 *
 *  So the service does the "join" manually:
 *  1. Fetch chats from chatRepository
 *  2. For each chat, find the OTHER participant's userId
 *  3. Fetch that user from userRepository
 *  4. Combine the data into ChatResponse
 *
 *  This is different from SQL where you'd write:
 *  SELECT chats.*, users.name FROM chats JOIN users ON ...
 *
 *  In Spring, we do it in Java code instead.
 *
 * ── NEW JAVA CONCEPT: Stream API ────────────────────────────────
 *
 *  You'll see .stream().map().collect() in the code below.
 *  This is Java's way to transform a List into another List.
 *
 *  Example:
 *  List<Chat> chats = [chat1, chat2, chat3]
 *
 *  List<ChatResponse> responses = chats
 *    .stream()                         → start processing the list
 *    .map(chat -> buildResponse(chat)) → convert each chat to ChatResponse
 *    .collect(Collectors.toList())     → collect results into a new List
 *
 *  It's like a factory assembly line — each item goes in, gets transformed, comes out.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public ChatService(ChatRepository chatRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    // ════════════════════════════════════════════════════════════
    //  GET ALL CHATS
    // ════════════════════════════════════════════════════════════

    /**
     * Returns all conversations for the logged-in user.
     * Sorted by most recent activity (latest message first).
     *
     * FLOW:
     * 1. Find all chats where participants contains requesterId
     * 2. For each chat, find the OTHER participant (the contact)
     * 3. Fetch contact's User object from MongoDB
     * 4. Build ChatResponse with combined data
     * 5. Return the list
     *
     * @param requesterId  the logged-in user's ID (from JWT token)
     */
    public List<ChatResponse> getAllChats(String requesterId) {
        log.debug("Fetching all chats for userId: {}", requesterId);

        // ── STEP 1: Get all chats for this user ─────────────────────
        // Already sorted by updatedAt descending (newest first)
        List<Chat> chats = chatRepository.findByParticipantsContainingOrderByUpdatedAtDesc(requesterId);
        log.debug("Found {} chats for userId: {}", chats.size(), requesterId);

        // ── STEP 2, 3, 4: For each chat, build a ChatResponse ────────
        // .stream()  → process list one by one
        // .map()     → convert each Chat → ChatResponse
        // .collect() → gather results back into a List
        return chats.stream()
                .map(chat -> buildChatResponse(chat, requesterId))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════
    //  CREATE NEW CHAT
    // ════════════════════════════════════════════════════════════

    /**
     * Starts a new conversation between two users.
     * If a chat already exists between them → returns the existing one.
     *
     * FLOW:
     * 1. Validate: can't start a chat with yourself
     * 2. Validate: the other person must exist
     * 3. Check: does a chat already exist between these two users?
     *    → YES: return the existing chat (don't create duplicate)
     *    → NO:  create a new Chat document and save it
     * 4. Return ChatResponse
     *
     * @param requesterId  logged-in user's ID (from JWT token)
     * @param request      contains the other person's ID (participantId)
     */
    public ChatResponse createChat(String requesterId, CreateChatRequest request) {
        log.debug("Creating chat between {} and {}", requesterId, request.participantId());

        // ── STEP 1: Can't chat with yourself ─────────────────────────
        if (requesterId.equals(request.participantId())) {
            throw new ApiException(
                    "You cannot start a chat with yourself.",
                    HttpStatus.BAD_REQUEST   // 400
            );
        }

        // ── STEP 2: Check the other person exists ─────────────────────
        // We don't want to create a chat with a non-existent user
        userRepository.findById(request.participantId())
                .orElseThrow(() -> new ApiException(
                        "User not found.",
                        HttpStatus.NOT_FOUND   // 404
                ));

        // ── STEP 3a: Check if chat already exists ─────────────────────
        // Uses @Query in repository: { 'participants': { $all: [id1, id2] } }
        // Returns Optional<Chat> — present if found, empty if not
        return chatRepository.findChatBetweenUsers(requesterId, request.participantId())
                .map(existingChat -> {
                    // Chat already exists — return it as-is (no duplicate!)
                    log.debug("Chat already exists: {}", existingChat.getId());
                    return buildChatResponse(existingChat, requesterId);
                })
                .orElseGet(() -> {
                    // ── STEP 3b: Create a brand new chat ─────────────────
                    // Arrays.asList creates [requesterId, participantId]
                    Chat newChat = new Chat(Arrays.asList(requesterId, request.participantId()));
                    Chat saved = chatRepository.save(newChat);
                    log.info("New chat created: {} between {} and {}",
                            saved.getId(), requesterId, request.participantId());
                    return buildChatResponse(saved, requesterId);
                });
    }

    // ════════════════════════════════════════════════════════════
    //  GET SINGLE CHAT BY ID
    // ════════════════════════════════════════════════════════════

    /**
     * Returns a single chat conversation by its ID.
     * Only participants can view their own chat (security check!).
     *
     * FLOW:
     * 1. Find chat by chatId in MongoDB
     * 2. SECURITY: verify requesterId is a participant
     * 3. Build and return ChatResponse
     *
     * @param chatId       the chat's MongoDB _id
     * @param requesterId  logged-in user's ID (from JWT token)
     */
    public ChatResponse getChatById(String chatId, String requesterId) {
        log.debug("Fetching chatId: {} for userId: {}", chatId, requesterId);

        // ── STEP 1: Find the chat ─────────────────────────────────────
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(
                        "Chat not found.",
                        HttpStatus.NOT_FOUND   // 404
                ));

        // ── STEP 2: SECURITY CHECK ────────────────────────────────────
        // Only participants can see this chat.
        // If someone knows a chatId but isn't part of it → BLOCKED!
        //
        // ANALOGY: You can't read someone else's WhatsApp messages
        // even if you somehow know the chat ID.
        if (!chat.getParticipants().contains(requesterId)) {
            throw new ApiException(
                    "You are not a participant of this chat.",
                    HttpStatus.FORBIDDEN   // 403
            );
        }

        // ── STEP 3: Return the chat ───────────────────────────────────
        return buildChatResponse(chat, requesterId);
    }

    // ════════════════════════════════════════════════════════════
    //  SEARCH CHATS
    // ════════════════════════════════════════════════════════════

    /**
     * Searches the logged-in user's chats by contact name OR last message text.
     *
     * HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────
     * 1. Get ALL chats for the user (same as getAllChats)
     * 2. For each chat, find the OTHER participant (the contact)
     * 3. Fetch their name from the users collection
     * 4. Keep chats where:
     *    - Contact name contains the search query (case-insensitive)
     *    - OR last message text contains the search query
     * 5. Map remaining chats to SearchResult and return
     *
     * JAVA CONCEPTS USED:
     * ─────────────────────────────────────────────────────────────
     * .stream().filter() → keeps only items matching a condition
     * .toLowerCase().contains() → case-insensitive substring search
     *
     * Example:
     * User searches "ali"
     * → "Alice Johnson".toLowerCase() = "alice johnson"
     * → "alice johnson".contains("ali") = TRUE ✅
     * → "Bob Smith".toLowerCase() = "bob smith"
     * → "bob smith".contains("ali") = FALSE ❌
     *
     * @param requesterId  the logged-in user's ID
     * @param query        the search keyword (minimum 2 characters)
     * @param limit        max results to return
     */
    public List<SearchResult> searchChats(String requesterId, String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            throw new ApiException(
                    "Search query must be at least 2 characters.",
                    HttpStatus.BAD_REQUEST
            );
        }

        String lowerQuery = query.toLowerCase().trim();
        log.debug("Searching chats for userId={} with query='{}'", requesterId, lowerQuery);

        List<Chat> allChats = chatRepository.findByParticipantsContainingOrderByUpdatedAtDesc(requesterId);

        return allChats.stream()
                .filter(chat -> {
                    // Find the contact's userId
                    String contactId = chat.getParticipants().stream()
                            .filter(id -> !id.equals(requesterId))
                            .findFirst().orElse(null);
                    if (contactId == null) return false;

                    User contact = userRepository.findById(contactId).orElse(null);

                    // Match 1: contact name contains the query
                    boolean nameMatch = contact != null &&
                            contact.getName().toLowerCase().contains(lowerQuery);

                    // Match 2: last message text contains the query
                    boolean messageMatch = chat.getLastMessage() != null &&
                            chat.getLastMessage().getText() != null &&
                            chat.getLastMessage().getText().toLowerCase().contains(lowerQuery);

                    return nameMatch || messageMatch;
                })
                .limit(limit)
                .map(chat -> {
                    String contactId = chat.getParticipants().stream()
                            .filter(id -> !id.equals(requesterId))
                            .findFirst().orElse(null);

                    User contact = contactId != null
                            ? userRepository.findById(contactId).orElse(null)
                            : null;

                    String contactName  = contact != null ? contact.getName() : "Unknown User";
                    String contactPhoto = contact != null ? contact.getProfilePhoto() : null;
                    String lastMsg      = chat.getLastMessage() != null
                            ? chat.getLastMessage().getText()
                            : null;

                    return new SearchResult(
                            chat.getId(),
                            contactName,
                            contactPhoto,
                            lastMsg,
                            chat.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════
    //  HELPER METHOD
    // ════════════════════════════════════════════════════════════

    /**
     * Converts a Chat model → ChatResponse DTO
     *
     * This is where we "JOIN" the chat with the user data:
     * - Figure out who the CONTACT is (the other participant)
     * - Fetch their name and photo from the users collection
     * - Build a complete ChatResponse
     *
     * PRIVATE method — only used inside this service.
     *
     * @param chat         the Chat document from MongoDB
     * @param requesterId  the logged-in user's ID
     */
    private ChatResponse buildChatResponse(Chat chat, String requesterId) {

        // ── Find the OTHER participant's ID ───────────────────────────
        // participants = [userId1, userId2]
        // If requesterId = userId1 → contact = userId2
        // If requesterId = userId2 → contact = userId1
        //
        // .stream().filter() goes through the list and keeps only items
        // that DON'T equal requesterId → that's the contact!
        String contactId = chat.getParticipants().stream()
                .filter(id -> !id.equals(requesterId))
                .findFirst()
                .orElse(null);

        // ── Fetch the contact's User from MongoDB ─────────────────────
        // We need their name and profilePhoto to show in the chat list
        String contactName         = "Unknown User";
        String contactProfilePhoto = null;

        if (contactId != null) {
            // Try to find the contact in the users collection
            // Use orElse(null) — if user was deleted, we handle it gracefully
            User contact = userRepository.findById(contactId).orElse(null);
            if (contact != null) {
                contactName         = contact.getName();
                contactProfilePhoto = contact.getProfilePhoto();
            }
        }

        // ── Extract last message info ─────────────────────────────────
        // lastMessage can be null (brand new chat, no messages yet)
        String        lastMessageText     = null;
        String        lastMessageSenderId = null;
        java.time.LocalDateTime lastMessageTime     = null;

        if (chat.getLastMessage() != null) {
            lastMessageText     = chat.getLastMessage().getText();
            lastMessageSenderId = chat.getLastMessage().getSenderId();
            lastMessageTime     = chat.getLastMessage().getTimestamp();
        }

        // ── Get unread count for the logged-in user ───────────────────
        // unreadCounts = { "userId1": 0, "userId2": 3 }
        // getOrDefault(requesterId, 0) → get my count, default 0 if not set
        int unreadCount = 0;
        if (chat.getUnreadCounts() != null) {
            unreadCount = chat.getUnreadCounts().getOrDefault(requesterId, 0);
        }

        // ── Build and return the ChatResponse ─────────────────────────
        return new ChatResponse(
                chat.getId(),
                contactId,
                contactName,
                contactProfilePhoto,
                lastMessageText,
                lastMessageSenderId,
                lastMessageTime,
                unreadCount,
                chat.getCreatedAt(),
                chat.getUpdatedAt()
        );
    }
}
