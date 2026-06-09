package com.chatapp.service;

import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.exception.ApiException;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════
 *  AuthService  —  THE BUSINESS LOGIC BRAIN
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT DOES A SERVICE DO?
 *  In Spring Boot architecture, the code has 3 layers:
 *
 *  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 *  │ CONTROLLER  │────▶│   SERVICE   │────▶│ REPOSITORY  │
 *  │  (Waiter)   │     │   (Chef)    │     │  (Fridge)   │
 *  └─────────────┘     └─────────────┘     └─────────────┘
 *   Receives HTTP        Does the actual      Reads/writes
 *   requests            business logic        MongoDB
 *   & returns JSON
 *
 *  The SERVICE contains all the rules and logic:
 *  - "Does this email already exist?"
 *  - "Do passwords match?"
 *  - "Hash the password before saving"
 *  - "Generate a JWT token"
 *
 * ── HOW DEPENDENCY INJECTION WORKS (without Lombok) ─────────────
 *
 *  Normally we'd use @RequiredArgsConstructor from Lombok to auto-generate
 *  the constructor. Since Lombok doesn't work with Java 25, we write it manually.
 *
 *  Spring sees our constructor that needs a UserRepository and JwtUtil,
 *  and automatically provides ("injects") those objects for us.
 *  We don't write: new UserRepository() — Spring handles that!
 */
@Service
public class AuthService {

    // ── LOGGER ────────────────────────────────────────────────────────────
    // Instead of Lombok's @Slf4j annotation, we declare the logger directly.
    // LoggerFactory creates a logger that knows this class's name.
    // Usage: log.info("message"), log.debug("message"), log.error("message")
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // ── DEPENDENCIES (injected by Spring) ────────────────────────────────
    private final UserRepository userRepository;
    private final JwtUtil        jwtUtil;

    /**
     * BCryptPasswordEncoder — the tool that:
     * 1. HASHES passwords at registration:   encode("mypassword") → "$2a$10$..."
     * 2. VERIFIES passwords at login:        matches("mypassword", "$2a$10$...") → true/false
     *
     * We create it here (not injected) because it has no state.
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ── CONSTRUCTOR (replaces @RequiredArgsConstructor) ───────────────────
    // Spring calls this constructor automatically when creating AuthService.
    // It passes in the already-created UserRepository and JwtUtil instances.
    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil        = jwtUtil;
    }

    // ════════════════════════════════════════════════════════════
    //  REGISTER  (FR-2.1 to FR-2.6)
    // ════════════════════════════════════════════════════════════

    /**
     * REGISTRATION FLOW — Step by step:
     *
     * Step 1: Check if passwords match  (FR-2.4)
     * Step 2: Check if email is already taken  (FR-2.6)
     * Step 3: Hash the password with BCrypt
     * Step 4: Build a User object with the data
     * Step 5: Save the User to MongoDB
     * Step 6: Return a success message
     *
     * @param request  contains name, email, password, confirmPassword
     *
     * NOTE ON RECORD SYNTAX:
     * RegisterRequest is a Java "record". Records use accessor methods WITHOUT "get".
     * So instead of request.getName() we write request.name()
     *      instead of request.getEmail() we write request.email()
     *      instead of request.getPassword() we write request.password()
     *      instead of request.getConfirmPassword() we write request.confirmPassword()
     */
    public String register(RegisterRequest request) {
        log.debug("Registration attempt for email: {}", request.email());

        // ── STEP 1: Passwords must match (FR-2.4) ──────────────────
        // We check this in Service because validation annotations
        // can't compare two different fields.
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(
                "Passwords do not match. Please try again.",
                HttpStatus.BAD_REQUEST   // 400
            );
        }

        // ── STEP 2: Is email already registered? (FR-2.6) ──────────
        // existsByEmail returns true if MongoDB already has this email.
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new ApiException(
                "An account with this email already exists. Please log in instead.",
                HttpStatus.CONFLICT     // 409 Conflict
            );
        }

        // ── STEP 3: Hash the password ────────────────────────────────
        // NEVER save the raw password!
        // BCrypt adds a random "salt" automatically (different hash each time
        // even for the same password — this prevents rainbow table attacks).
        //
        // Example:
        // passwordEncoder.encode("hello123")
        // → "$2a$10$X.2jqNMGVjGLbDM6z0mj2u9Kk4bY8ZxJr7GE6q..."
        String hashedPassword = passwordEncoder.encode(request.password());
        log.debug("Password hashed successfully");

        // ── STEP 4: Build the User object ────────────────────────────
        // We create an empty User and set each field using setters.
        // (Previously used Lombok's @Builder pattern — same result, different syntax.)
        User newUser = new User();
        newUser.setName(request.name().trim());
        newUser.setEmail(request.email().toLowerCase().trim());
        newUser.setPassword(hashedPassword);          // hashed, not plain!
        newUser.setCreatedAt(LocalDateTime.now());    // set creation timestamp

        // ── STEP 5: Save to MongoDB ──────────────────────────────────
        // userRepository.save() does an INSERT (because id is null/new).
        // After saving, MongoDB sets the 'id' field automatically.
        User savedUser = userRepository.save(newUser);
        log.info("New user registered: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // ── STEP 6: Return success ───────────────────────────────────
        return "Account created successfully! Please log in.";
    }

    // ════════════════════════════════════════════════════════════
    //  LOGIN  (FR-1.1 to FR-1.3)
    // ════════════════════════════════════════════════════════════

    /**
     * LOGIN FLOW — Step by step:
     *
     * Step 1: Find the user by email
     * Step 2: Verify the password matches the stored BCrypt hash
     * Step 3: Generate a JWT token
     * Step 4: Return token + user info to the frontend
     *
     * @param request  contains email and password
     * @return         AuthResponse with JWT token and user details
     */
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.email());

        // ── STEP 1: Find user by email ───────────────────────────────
        // findByEmail returns Optional<User>.
        // .orElseThrow() → if empty (user not found), throw our custom exception.
        //
        // IMPORTANT: We say "Invalid email or password" instead of
        // "Email not found" — this is intentional!
        // If we said "email not found", a hacker could use it to discover
        // which emails are registered. Keep error messages vague for security.
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApiException(
                    "Invalid email or password. Please try again.",
                    HttpStatus.UNAUTHORIZED  // 401
                ));

        // ── STEP 2: Verify the password ──────────────────────────────
        // passwordEncoder.matches() compares:
        // - request.password()   → what the user just typed ("hello123")
        // - user.getPassword()   → the BCrypt hash stored in MongoDB
        //
        // BCrypt extracts the original salt from the hash and re-hashes
        // the entered password with it. If the result matches, passwords are equal.
        boolean passwordMatches = passwordEncoder.matches(
            request.password(),
            user.getPassword()
        );

        if (!passwordMatches) {
            // Same vague message as above — don't reveal if it's the email or password
            throw new ApiException(
                "Invalid email or password. Please try again.",
                HttpStatus.UNAUTHORIZED  // 401
            );
        }

        // ── STEP 3: Generate JWT token ───────────────────────────────
        // Credentials are correct! Time to issue the "wristband".
        String token = jwtUtil.generateToken(
            user.getId(),    // subject: user's MongoDB id
            user.getEmail(),
            user.getName()
        );
        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());

        // ── STEP 4: Build and return the response ────────────────────
        // AuthResponse is a Java record — we create it with its constructor.
        // Argument order: token, userId, name, email
        // (Previously used AuthResponse.builder()...build() — same result!)
        return new AuthResponse(
            token,
            user.getId(),
            user.getName(),
            user.getEmail()
        );
    }
}
