package com.chatapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════
 *  Message  —  BLUEPRINT for one message in MongoDB
 * ════════════════════════════════════════════════════════════════
 *
 *  One Message document = one single message in a conversation.
 *
 *  REAL MONGODB DOCUMENT LOOKS LIKE THIS:
 *  {
 *    "_id":       "64abc4560000000000000003",
 *    "chatId":    "64abc4560000000000000002",
 *    "senderId":  "64abc789...",
 *    "text":      "Hey, how are you?",
 *    "type":      "text",
 *    "fileUrl":   null,
 *    "timestamp": "2026-06-09T10:30:00",
 *    "read":      false
 *  }
 *
 *  WHY A SEPARATE COLLECTION FROM CHATS?
 *  ─────────────────────────────────────
 *  MongoDB documents have a 16 MB size limit.
 *  If we stored ALL messages inside the Chat document, a chat with
 *  10,000 messages would eventually hit this limit.
 *
 *  SOLUTION: Messages get their own collection.
 *  Each message has a chatId field pointing back to the parent chat.
 *  This is similar to how a SQL foreign key works:
 *    messages.chatId → chats._id
 *
 *  INDEXES:
 *  ─────────────────────────────────────
 *  @CompoundIndex on (chatId + timestamp):
 *  When loading chat history, we always query:
 *    "find all messages WHERE chatId = X ORDER BY timestamp"
 *  A compound index on both fields makes this query instant.
 */
@Document(collection = "messages")
@CompoundIndex(name = "chat_timestamp_idx", def = "{'chatId': 1, 'timestamp': 1}")
public class Message {

    @Id
    private String id;

    /** Which conversation this message belongs to */
    @Indexed
    private String chatId;

    /** The userId of who sent this message */
    private String senderId;

    /** The message content (null for file-only messages) */
    private String text;

    /**
     * Message type — controls how the frontend renders it.
     * "text"  → plain text bubble
     * "image" → show image preview
     * "file"  → show file download link
     * "audio" → show audio player
     */
    private String type;

    /** URL to the uploaded file (null for type="text") */
    private String fileUrl;

    /** When the message was sent */
    private LocalDateTime timestamp;

    /**
     * Has the recipient read this message?
     * true  = they opened the chat and saw it
     * false = unread (shows the blue unread badge)
     */
    private boolean read;

    // ── NO-ARG CONSTRUCTOR ─────────────────────────────────────────────────
    public Message() {}

    // ── CONSTRUCTOR for a new text message ────────────────────────────────
    public Message(String chatId, String senderId, String text) {
        this.chatId    = chatId;
        this.senderId  = senderId;
        this.text      = text;
        this.type      = "text";
        this.fileUrl   = null;
        this.timestamp = LocalDateTime.now();
        this.read      = false;
    }

    // ── GETTERS ────────────────────────────────────────────────────────────
    public String getId()              { return id; }
    public String getChatId()          { return chatId; }
    public String getSenderId()        { return senderId; }
    public String getText()            { return text; }
    public String getType()            { return type; }
    public String getFileUrl()         { return fileUrl; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public boolean isRead()            { return read; }

    // ── SETTERS ────────────────────────────────────────────────────────────
    public void setId(String id)                      { this.id        = id; }
    public void setChatId(String chatId)              { this.chatId    = chatId; }
    public void setSenderId(String senderId)          { this.senderId  = senderId; }
    public void setText(String text)                  { this.text      = text; }
    public void setType(String type)                  { this.type      = type; }
    public void setFileUrl(String fileUrl)            { this.fileUrl   = fileUrl; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read)                 { this.read      = read; }
}
