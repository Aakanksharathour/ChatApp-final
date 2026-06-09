package com.chatapp.dto;

/**
 * ════════════════════════════════════════════════════════════════
 *  UserProfileResponse  —  What we SEND BACK for GET /api/user/:id
 * ════════════════════════════════════════════════════════════════
 *
 *  When someone asks "give me user with id=123", we return this:
 *  {
 *    "id":           "6a1e24792a468463aee9be12",
 *    "name":         "Alice Johnson",
 *    "email":        "alice@example.com",
 *    "profilePhoto": "https://cdn.chatapp.com/photos/u123.jpg"
 *  }
 *
 *  WHY NOT return the User object directly?
 *  Because User has a 'password' field (BCrypt hash).
 *  NEVER send password — even hashed — to the frontend!
 *  This DTO is a safe "filtered" version of User.
 *
 *  It's a Java Record → auto-generates constructor, accessors, equals, toString.
 */
public record UserProfileResponse(
    String id,
    String name,
    String email,
    String profilePhoto
) {}
