package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════
 *  LastMessage  —  EMBEDDED OBJECT inside a Chat document
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT IS AN EMBEDDED DOCUMENT?
 *  ──────────────────────────────
 *  In MongoDB, a document can contain another object INSIDE it.
 *  LastMessage is NOT a separate collection — it's a nested object
 *  stored INSIDE each chat document.
 *
 *  WHY DO WE STORE LAST MESSAGE INSIDE CHAT?
 *  ──────────────────────────────────────────
 *  When the Chat List Screen loads, it shows:
 *  - Contact name
 *  - Last message preview ("Hey, how are you?")
 *  - Time of last message
 *
 *  If we didn't cache this, we'd need to query the ENTIRE messages
 *  collection for every chat just to get the last message.
 *  For 50 chats → 50 extra queries = VERY SLOW!
 *
 *  By storing lastMessage INSIDE the chat document, we get it
 *  for FREE when we fetch the chat. This is called DENORMALIZATION.
 *
 *  ANALOGY: Instead of going to the library (messages collection)
 *  every time you want to know the latest news, you keep a sticky note
 *  (lastMessage) on your desk that gets updated whenever new news arrives.
 *
 *  This class has NO @Document annotation because it's not a
 *  separate collection — it's just a Java class that Spring Data
 *  MongoDB automatically embeds inside the Chat document.
 *
 *  MongoDB document structure:
 *  {
 *    "_id": "chatId",
 *    "participants": ["userId1", "userId2"],
 *    "lastMessage": {
 *      "text":      "Hey, how are you?",
 *      "senderId":  "userId1",
 *      "timestamp": "2026-06-09T10:30:00"
 *    }
 *  }
 */
public class LastMessage {

    /** The text of the last message sent in this chat */
    private String text;

    /** The userId of whoever sent the last message */
    private String senderId;

    /** When the last message was sent */
    private LocalDateTime timestamp;

    // ── NO-ARG CONSTRUCTOR ────────────────────────────────────────────────
    public LastMessage() {}

    // ── ALL-ARG CONSTRUCTOR ───────────────────────────────────────────────
    public LastMessage(String text, String senderId, LocalDateTime timestamp) {
        this.text      = text;
        this.senderId  = senderId;
        this.timestamp = timestamp;
    }

    // ── GETTERS ───────────────────────────────────────────────────────────
    public String getText()            { return text; }
    public String getSenderId()        { return senderId; }
    public LocalDateTime getTimestamp(){ return timestamp; }

    // ── SETTERS ───────────────────────────────────────────────────────────
    public void setText(String text)                    { this.text      = text; }
    public void setSenderId(String senderId)            { this.senderId  = senderId; }
    public void setTimestamp(LocalDateTime timestamp)   { this.timestamp = timestamp; }
}
