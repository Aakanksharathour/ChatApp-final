package com.chatapp.dto;

/**
 * ════════════════════════════════════════════════════════════════
 *  AuthResponse  —  What we SEND BACK to the frontend after login
 * ════════════════════════════════════════════════════════════════
 *
 *  After a successful LOGIN, our API returns this as JSON:
 *  {
 *    "token":   "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NWYxY...",
 *    "userId":  "65f1b2c3d4e5f6a7b8c9d0e1",
 *    "name":    "Alice Johnson",
 *    "email":   "alice@example.com"
 *  }
 *
 *  The FRONTEND stores this token (in localStorage or memory).
 *  For every future API call (like "get my chats"), the frontend
 *  sends this token in the HTTP header:
 *  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 *
 *  The backend then verifies the token and knows who is calling.
 *
 *  WHY NOT SEND BACK THE PASSWORD?
 *  NEVER include passwords (even hashed ones) in API responses!
 *  That would be a huge security vulnerability.
 *
 *  WHY A JAVA RECORD?
 *  A Java record auto-generates the constructor, accessors, equals, hashCode, and toString.
 *  To create one: new AuthResponse(token, userId, name, email)
 *  To read fields: response.token(), response.userId(), etc. (no "get" prefix)
 *
 *  Spring automatically converts this to JSON when we return it from a @RestController.
 *  Jackson (the JSON library) reads the record components and serializes them.
 */
public record AuthResponse(

    /**
     * The JWT token string.
     * Frontend saves this and sends it with future API requests.
     * Example: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2..."
     */
    String token,

    /**
     * The user's MongoDB ID.
     * Frontend can use this to reference the logged-in user.
     */
    String userId,

    /**
     * The user's display name. Frontend shows this in the UI.
     * Example: "Alice Johnson"
     */
    String name,

    /**
     * The user's email. Frontend might display this in the profile screen.
     */
    String email

) {}
