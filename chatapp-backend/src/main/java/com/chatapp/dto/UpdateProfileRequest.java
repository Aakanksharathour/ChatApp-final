package com.chatapp.dto;

/**
 * ════════════════════════════════════════════════════════════════
 *  UpdateProfileRequest  —  What frontend SENDS for PUT /api/user/:id
 * ════════════════════════════════════════════════════════════════
 *
 *  Frontend sends this JSON:
 *  {
 *    "name":         "Alice Updated",
 *    "profilePhoto": "https://cdn.chatapp.com/photos/u123.jpg"
 *  }
 *
 *  Both fields are OPTIONAL — user can update just the name,
 *  just the photo, or both at the same time.
 *
 *  No @NotBlank here because both fields are optional updates.
 *  We check in the Service: if name is provided → update it.
 *                           if profilePhoto is provided → update it.
 */
public record UpdateProfileRequest(
    String name,
    String profilePhoto
) {}
