package com.chatapp.repository;

import com.chatapp.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════
 *  MessageRepository  —  DATABASE QUERIES for Messages
 * ════════════════════════════════════════════════════════════════
 *
 *  Spring Data MongoDB generates the SQL-equivalent queries
 *  automatically from the method name. No SQL needed!
 *
 *  HOW METHOD NAMES BECOME QUERIES:
 *  ─────────────────────────────────
 *  findByChatIdOrderByTimestampAsc(chatId)
 *  → db.messages.find({ chatId: chatId }).sort({ timestamp: 1 })
 *
 *  Method name parts:
 *   findBy     = SELECT ... WHERE
 *   ChatId     = the field to filter on
 *   OrderBy    = ORDER BY
 *   Timestamp  = the field to sort on
 *   Asc        = ascending (oldest first, like a real chat history)
 *
 *  We also support PAGINATION via Pageable:
 *  findByChatIdOrderByTimestampDesc(chatId, pageable)
 *  → loads messages in REVERSE order (newest first), then we flip them
 *  → this is how WhatsApp loads chat history: newest messages first,
 *    and you scroll UP to load older ones
 */
public interface MessageRepository extends MongoRepository<Message, String> {

    /**
     * Get ALL messages in a chat, oldest first (for initial load).
     * Used when loading full chat history.
     *
     * MongoDB equivalent:
     * db.messages.find({ chatId: chatId }).sort({ timestamp: 1 })
     */
    List<Message> findByChatIdOrderByTimestampAsc(String chatId);

    /**
     * Get messages with PAGINATION, newest first.
     * Pageable controls: page number + page size.
     *
     * Example: page=0, size=50 → last 50 messages
     *          page=1, size=50 → the 50 messages before that
     *
     * MongoDB equivalent:
     * db.messages.find({ chatId: chatId }).sort({ timestamp: -1 }).skip(page*size).limit(size)
     */
    List<Message> findByChatIdOrderByTimestampDesc(String chatId, Pageable pageable);
}
