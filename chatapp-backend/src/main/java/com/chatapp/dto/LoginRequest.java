package com.chatapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * ════════════════════════════════════════════════════════════════
 *  LoginRequest  —  DTO for the Login endpoint
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT THE FRONTEND SENDS (JSON):
 *  POST /api/auth/login
 *  {
 *    "email":    "alice@example.com",
 *    "password": "mypassword123"
 *  }
 *
 *  Only two fields needed for login — email and password.
 *  Much simpler than RegisterRequest!
 *
 *  WHY A JAVA RECORD?
 *  See RegisterRequest.java for full explanation.
 *  Access fields as: request.email(), request.password()
 *  (NO "get" prefix — that's how Java records work!)
 */
public record LoginRequest(

    /**
     * The user's registered email. (FR-1.1)
     * Must be a valid email format.
     */
    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    String email,

    /**
     * The plain-text password entered by the user. (FR-1.2)
     * We will compare this against the stored BCrypt hash in the Service.
     */
    @NotBlank(message = "Password is required.")
    String password

) {}
