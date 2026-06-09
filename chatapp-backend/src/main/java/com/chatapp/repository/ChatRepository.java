package com.chatapp.repository;

import com.chatapp.model.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * ════════════════════════════════════════════════════════════════
 *  ChatRepository  —  TV REMOTE for the chats collection
 * ════════════════════════════════════════════════════════════════
 *
 *  Extends MongoRepository<Chat, String> which gives us free methods:
 *  - save(chat)         → INSERT or UPDATE
 *  - findById(id)       → find by _id
 *  - delete(chat)       → DELETE
 *  - findAll()          → get everything (we don't use this for chats)
 *
 *  We add TWO custom methods:
 *
 *  1. findByParticipantsContainingOrderByUpdatedAtDesc(userId)
 *     → "Find all chats where the participants array contains this userId,
 *        sorted by most recent activity first"
 *     → Spring reads the method name and generates the MongoDB query:
 *        { "participants": { "$in": [userId] } }   sorted by updatedAt DESC
 *
 *  2. findChatBetweenUsers(userId1, userId2)  — uses @Query
 *     → "Find the chat where BOTH userId1 AND userId2 are participants"
 *     → Used when creating a chat to check if one already exists
 *     → MongoDB query: { "participants": { "$all": ["userId1", "userId2"] } }
 *
 * ── NEW CONCEPT: @Query ──────────────────────────────────────────
 *
 *  Sometimes Spring can't auto-generate the query from the method name.
 *  For example: "find where array contains BOTH of these values".
 *  That's when we use @Query and write the MongoDB query manually.
 *
 *  @Query("{ 'participants': { $all: [?0, ?1] } }")
 *  ?0 = first parameter (userId1)
 *  ?1 = second parameter (userId2)
 *
 *  $all = MongoDB operator meaning "array must contain ALL of these values"
 *
 *  Compare:
 *  $in  → array contains AT LEAST ONE of these values
 *  $all → array contains ALL of these values (what we want here)
 */
public interface ChatRepository extends MongoRepository<Chat, String> {

    /**
     * GET ALL CHATS FOR A USER
     * ─────────────────────────────────────────────────────────────
     * Finds all chats where the participants array contains the given userId.
     * Results are sorted by updatedAt (most recent conversation first).
     *
     * Spring generates this MongoDB query from the method name:
     * { "participants": userId }   → sorted by updatedAt descending
     *
     * Method name breakdown:
     * findBy          → SELECT WHERE
     * Participants    → field name
     * Containing      → array contains this value ($elemMatch)
     * OrderBy         → ORDER BY
     * UpdatedAt       → field name
     * Desc            → descending (newest first)
     *
     * @param userId  the logged-in user's ID
     * @return        list of all their chats, newest first
     */
    List<Chat> findByParticipantsContainingOrderByUpdatedAtDesc(String userId);

    /**
     * CHECK IF CHAT ALREADY EXISTS BETWEEN TWO USERS
     * ─────────────────────────────────────────────────────────────
     * Used before creating a new chat to avoid duplicates.
     *
     * @Query writes the MongoDB query manually:
     * { 'participants': { $all: ['userId1', 'userId2'] } }
     *
     * $all means: "participants array must contain BOTH userId1 AND userId2"
     *
     * ?0 = first parameter (userId1)
     * ?1 = second parameter (userId2)
     *
     * @param userId1  first user's ID
     * @param userId2  second user's ID
     * @return         Optional<Chat> — present if chat exists, empty if not
     */
    @Query("{ 'participants': { $all: [?0, ?1] } }")
    Optional<Chat> findChatBetweenUsers(String userId1, String userId2);
}
