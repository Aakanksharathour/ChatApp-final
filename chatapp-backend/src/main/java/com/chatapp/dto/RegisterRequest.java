package com.chatapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ════════════════════════════════════════════════════════════════
 *  RegisterRequest  —  DTO (Data Transfer Object)
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT IS A DTO?
 *  DTO = Data Transfer Object.
 *  It's just a simple class that CARRIES data from the frontend to the backend.
 *
 *  ANALOGY: Think of a DTO like an ORDER SLIP at a restaurant.
 *  The customer (frontend) writes their order on the slip (fills in JSON fields).
 *  The waiter (Controller) picks it up and hands it to the chef (Service).
 *
 *  WHY NOT use the User model directly?
 *  Because the User model has fields like 'id', 'createdAt' that the
 *  frontend should NOT be able to set. The DTO only exposes what we want.
 *
 *  WHAT THE FRONTEND SENDS (JSON):
 *  POST /api/auth/register
 *  {
 *    "name":            "Alice Johnson",
 *    "email":           "alice@example.com",
 *    "password":        "mypassword123",
 *    "confirmPassword": "mypassword123"
 *  }
 *
 *  WHY A JAVA RECORD?
 *  A Java "record" (available since Java 16) is a super-compact way to
 *  write a data-carrying class. It automatically gives you:
 *  - Constructor: new RegisterRequest(name, email, password, confirmPassword)
 *  - Accessors:   request.name(), request.email(), request.password(), request.confirmPassword()
 *                 NOTE: Records use method-style accessors WITHOUT "get" prefix!
 *                 So it's request.name() — NOT request.getName()
 *  - equals(), hashCode(), toString()
 *  All with ZERO extra code. Perfect for DTOs!
 *
 * ── VALIDATION ANNOTATIONS ──────────────────────────────────────
 *
 *  These are checked AUTOMATICALLY before our service code runs.
 *  If any fail, Spring returns a 400 Bad Request with error details.
 *  No manual if-else needed!
 *
 *  @NotBlank  → field cannot be null or empty string (not even spaces)
 *  @Email     → must be a valid email format (contains @ and a domain)
 *  @Size      → length constraints
 */
public record RegisterRequest(

    /**
     * Full name of the user. (FR-2.1)
     * @NotBlank → cannot be empty
     * message   → custom error message shown to the user if validation fails
     */
    @NotBlank(message = "Name is required.")
    String name,

    /**
     * Email address. (FR-2.2)
     * @NotBlank → cannot be empty
     * @Email    → must be a valid format like "user@domain.com"
     */
    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    String email,

    /**
     * Password. (FR-2.3)
     * @Size(min=6) → password must be at least 6 characters long
     */
    @NotBlank(message = "Password is required.")
    @Size(min = 6, message = "Password must be at least 6 characters.")
    String password,

    /**
     * Confirm password. (FR-2.4)
     * We check that this matches 'password' in the Service layer
     * (not via annotation because it requires comparing two fields).
     */
    @NotBlank(message = "Please confirm your password.")
    String confirmPassword

) {}
