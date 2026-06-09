package com.chatapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════
 *  Chat  —  BLUEPRINT for one conversation in MongoDB
 * ════════════════════════════════════════════════════════════════
 *
 *  One Chat document = one conversation between exactly 2 users.
 *
 *  REAL MONGODB DOCUMENT LOOKS LIKE THIS:
 *  {
 *    "_id":          "64abc4560000000000000002",
 *    "participants": ["userId1", "userId2"],
 *    "lastMessage":  {
 *                      "text":      "Hey, how are you?",
 *                      "senderId":  "userId1",
 *                      "timestamp": "2026-06-09T10:30:00Z"
 *                    },
 *    "unreadCounts": { "userId1": 0, "userId2": 3 },
 *    "createdAt":    "2026-01-15T00:00:00Z",
 *    "updatedAt":    "2026-06-09T10:30:00Z"
 *  }
 *
 * ── KEY DESIGN DECISIONS ────────────────────────────────────────
 *
 *  1. participants = List<String>
 *     An array of exactly 2 user IDs. When user A fetches their chats,
 *     we find all chats where participants array CONTAINS A's userId.
 *     The OTHER person in the array is their contact.
 *
 *  2. lastMessage = LastMessage (embedded object)
 *     Cached here to avoid querying messages collection every time.
 *     Updated every time a new message is sent.
 *
 *  3. unreadCounts = Map<String, Integer>
 *     Tracks how many unread messages each user has.
 *     Example: { "userId1": 0, "userId2": 3 }
 *     → userId1 has read everything, userId2 has 3 unread messages.
 *     When user opens the chat → reset their count to 0.
 *
 *  4. updatedAt is indexed (descending) for sorting
 *     Chat List shows most recently active conversations first.
 *     Without this index, sorting would be very slow for large datasets.
 *
 * ── ANNOTATIONS ─────────────────────────────────────────────────
 *
 *  @CompoundIndex → creates a unique index on the participants array.
 *  This prevents creating duplicate chats between the same two users.
 *  { unique=true } means MongoDB will reject a second chat with
 *  participants = [A, B] if one already exists.
 */
@Document(collection = "chats")
@CompoundIndex(name = "unique_chat_participants", def = "{'participants': 1}", unique = true)
public class Chat {

    @Id
    private String id;

    /**
     * Array of exactly 2 user IDs.
     * Example: ["6a1e24792a468463aee9be12", "6b2f35803b579574bff0cf23"]
     *
     * @Indexed → creates an index on this field.
     * When we query "find all chats where participants contains userId",
     * MongoDB uses this index to find results instantly instead of
     * scanning the entire collection.
     */
    @Indexed
    private List<String> participants;

    /**
     * Embedded last message — cached here for performance.
     * Updated every time a new message is sent in this chat.
     * null if no messages have been sent yet (brand new chat).
     */
    private LastMessage lastMessage;

    /**
     * Unread message count per user.
     * Map<userId, unreadCount>
     * Example: { "userId1": 0, "userId2": 3 }
     *
     * Map<String, Integer> in Java becomes a JSON object in MongoDB:
     * { "key1": value1, "key2": value2 }
     */
    private Map<String, Integer> unreadCounts;

    /** When this conversation was first created */
    private LocalDateTime createdAt;

    /**
     * Last time this conversation had activity (new message sent).
     * Used to sort the Chat List — most recent conversation shows first.
     *
     * @Indexed → sorted index on this field for fast "ORDER BY updatedAt DESC"
     */
    @Indexed
    private LocalDateTime updatedAt;

    // ── NO-ARG CONSTRUCTOR ────────────────────────────────────────────────
    public Chat() {}

    // ── CONSTRUCTOR for creating a NEW chat ──────────────────────────────
    // Used in ChatService when two users start a conversation for the first time.
    public Chat(List<String> participants) {
        this.participants  = participants;
        this.lastMessage   = null;                   // no messages yet
        this.unreadCounts  = new HashMap<>();        // empty map — no unreads yet
        this.createdAt     = LocalDateTime.now();
        this.updatedAt     = LocalDateTime.now();
    }

    // ── GETTERS ───────────────────────────────────────────────────────────
    public String getId()                          { return id; }
    public List<String> getParticipants()          { return participants; }
    public LastMessage getLastMessage()            { return lastMessage; }
    public Map<String, Integer> getUnreadCounts()  { return unreadCounts; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }

    // ── SETTERS ───────────────────────────────────────────────────────────
    public void setId(String id)                                    { this.id           = id; }
    public void setParticipants(List<String> participants)          { this.participants  = participants; }
    public void setLastMessage(LastMessage lastMessage)             { this.lastMessage   = lastMessage; }
    public void setUnreadCounts(Map<String, Integer> unreadCounts)  { this.unreadCounts  = unreadCounts; }
    public void setCreatedAt(LocalDateTime createdAt)               { this.createdAt     = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)               { this.updatedAt     = updatedAt; }
}
