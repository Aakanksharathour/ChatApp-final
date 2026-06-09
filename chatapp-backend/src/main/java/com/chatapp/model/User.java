package com.chatapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════
 *  User  —  THE BLUEPRINT for what a User looks like in MongoDB
 * ════════════════════════════════════════════════════════════════
 *
 *  ANALOGY: Think of this class like a FORM template.
 *  Every user we save fills in one copy of this form.
 *  MongoDB stores each filled-in form as a "document" in the "users" collection.
 *
 *  In MongoDB:
 *  - DATABASE   = chatapp              (like a filing cabinet)
 *  - COLLECTION = users                (like a drawer in that cabinet)
 *  - DOCUMENT   = one user's data     (like a single paper in that drawer)
 *
 *  Example of what one document looks like in MongoDB:
 *  {
 *    "_id":       "65f1b2c3d4e5f6a7b8c9d0e1",
 *    "name":      "John Doe",
 *    "email":     "john@example.com",
 *    "password":  "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
 *    "createdAt": "2026-06-01T10:30:00"
 *  }
 *
 *  WHY NO LOMBOK?
 *  Lombok (a code-generation library) doesn't work with Java 25 due to
 *  internal compiler API changes. So we write getters/setters manually.
 *  It's more typing but identical in behavior.
 */
@Document(collection = "users")
public class User {

    /**
     * @Id → PRIMARY KEY. MongoDB auto-generates a unique string like "65f1b2c3d4e5f6a7b8c9d0e1"
     */
    @Id
    private String id;

    /** The user's display name. Example: "Alice Johnson" */
    private String name;

    /**
     * @Indexed(unique = true) → Two effects:
     * 1. Creates a fast-lookup index on this field
     * 2. MongoDB rejects duplicate emails — enforces SRS FR-2.6
     */
    @Indexed(unique = true)
    private String email;

    /**
     * ALWAYS stored as a BCrypt hash — NEVER plain text.
     * Example stored value: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     * Satisfies SRS NFR 4.2: "Passwords shall be stored using bcrypt with salt"
     */
    private String password;

    /**
     * URL or base64 string of the user's profile photo.
     * Example: "https://cdn.chatapp.com/photos/u123.jpg"
     * Can be null if the user hasn't set one yet.
     */
    private String profilePhoto;

    /** Timestamp of when this account was created. Set in AuthService during registration. */
    private LocalDateTime createdAt;

    // ── NO-ARG CONSTRUCTOR ────────────────────────────────────────────────
    public User() {}

    // ── ALL-ARG CONSTRUCTOR ───────────────────────────────────────────────
    public User(String id, String name, String email, String password, String profilePhoto, LocalDateTime createdAt) {
        this.id           = id;
        this.name         = name;
        this.email        = email;
        this.password     = password;
        this.profilePhoto = profilePhoto;
        this.createdAt    = createdAt;
    }

    // ── GETTERS ───────────────────────────────────────────────────────────
    // Getters let other classes READ the field values. Example: user.getId()

    public String getId()              { return id; }
    public String getName()            { return name; }
    public String getEmail()           { return email; }
    public String getPassword()        { return password; }
    public String getProfilePhoto()    { return profilePhoto; }
    public LocalDateTime getCreatedAt(){ return createdAt; }

    // ── SETTERS ───────────────────────────────────────────────────────────
    // Setters let other classes SET/CHANGE field values. Example: user.setName("Bob")
    // Spring Data MongoDB uses these internally when loading documents from the DB.

    public void setId(String id)                     { this.id           = id; }
    public void setName(String name)                 { this.name         = name; }
    public void setEmail(String email)               { this.email        = email; }
    public void setPassword(String password)         { this.password     = password; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt    = createdAt; }

    // ── toString ──────────────────────────────────────────────────────────
    // Useful for logging — e.g., log.info("user={}", user) prints this.
    // NOTE: password is intentionally excluded from toString for security.
    @Override
    public String toString() {
        return "User{id='" + id + "', name='" + name + "', email='" + email + "', createdAt=" + createdAt + "}";
    }
}
