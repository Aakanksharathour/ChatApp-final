package com.chatapp.dto;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════
 *  SearchResult  —  One item in the search results list
 * ════════════════════════════════════════════════════════════════
 *
 *  When the user searches for "Alice", the API returns a list of these.
 *
 *  RESPONSE EXAMPLE:
 *  {
 *    "results": [
 *      {
 *        "chatId":              "64abc456...",
 *        "contactName":         "Alice Johnson",
 *        "contactProfilePhoto": null,
 *        "matchedMessage":      "Hey, how are you?",
 *        "timestamp":           "2026-05-31T10:30:00"
 *      }
 *    ]
 *  }
 *
 *  matchedMessage = the last message text (used for context in search results).
 *  null if no messages have been sent yet in this chat.
 */
public record SearchResult(

    String chatId,
    String contactName,
    String contactProfilePhoto,
    String matchedMessage,
    LocalDateTime timestamp

) {}
