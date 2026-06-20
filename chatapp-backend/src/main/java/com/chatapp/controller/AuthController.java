package com.chatapp.controller;

import com.chatapp.dto.AuthResponse;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════
 *  AuthController  —  THE FRONT DOOR (HTTP Endpoint Handler)
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT IS A CONTROLLER?
 *  A Controller is the part of your backend that LISTENS for HTTP requests
 *  and decides what to do with them.
 *
 *  ANALOGY: The Controller is like a RECEPTIONIST at a hospital.
 *  - Patient (frontend) walks in and says: "I want to register"
 *  - Receptionist (Controller) says: "OK, fill this form (DTO)"
 *  - Receptionist passes form to the Doctor (Service)
 *  - Doctor does the work and gives back results
 *  - Receptionist gives the result back to the patient (HTTP response)
 *
 * ── ANNOTATIONS ─────────────────────────────────────────────────
 *
 *  @RestController → Combines:
 *    1. @Controller   = this class handles HTTP requests
 *    2. @ResponseBody = automatically convert return values to JSON
 *    So when we return an object, Spring converts it to JSON automatically.
 *
 *  @RequestMapping("/api/auth") → All endpoints in this controller
 *    start with /api/auth. So our full URLs become:
 *    POST http://localhost:8080/api/auth/register
 *    POST http://localhost:8080/api/auth/login
 *
 *  @CrossOrigin → CORS (Cross-Origin Resource Sharing).
 *    By default, browsers BLOCK requests from one domain to another.
 *    Our frontend runs on localhost:5173 and backend on localhost:8080
 *    — different "origins"! @CrossOrigin tells the browser:
 *    "Hey, it's OK! localhost:5173 is allowed to call me."
 *    Without this, the frontend would get a CORS error.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Spring automatically provides (injects) this AuthService instance. */
    private final AuthService authService;

    // ── CONSTRUCTOR (replaces @RequiredArgsConstructor) ───────────────────
    // Spring calls this constructor and passes in the AuthService it manages.
    // This is called "Constructor Injection" — the recommended way to wire dependencies.
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ════════════════════════════════════════════════════════════
    //  POST /api/auth/register
    // ════════════════════════════════════════════════════════════

    /**
     * REGISTRATION ENDPOINT
     *
     * URL:     POST http://localhost:8080/api/auth/register
     * Access:  Public (no token needed — you don't have one yet!)
     *
     * REQUEST BODY (JSON):
     * {
     *   "name":            "Alice Johnson",
     *   "email":           "alice@example.com",
     *   "password":        "mypassword123",
     *   "confirmPassword": "mypassword123"
     * }
     *
     * SUCCESS RESPONSE (201 Created):
     * { "message": "Account created successfully! Please log in." }
     *
     * ERROR RESPONSES:
     * 400 Bad Request  → validation failed (empty fields, invalid email, etc.)
     * 409 Conflict     → email already registered
     *
     * ── HOW IT WORKS ────────────────────────────────────────────
     *
     * @PostMapping("/register")   → listens for POST requests to /api/auth/register
     *
     * @RequestBody RegisterRequest request
     *   → Spring reads the JSON body and converts it into a RegisterRequest object.
     *   → Like filling in a form object from JSON automatically.
     *
     * @Valid
     *   → Runs the validation annotations on RegisterRequest BEFORE this method runs.
     *   → If anything is invalid (@NotBlank, @Email, @Size), Spring throws
     *     MethodArgumentNotValidException which our GlobalExceptionHandler catches.
     *
     * ResponseEntity<Map<String, String>>
     *   → ResponseEntity lets us control BOTH the response body AND the HTTP status code.
     *   → Map<String, String> = a JSON object like { "key": "value" }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        String message = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", message));
    }

    // ════════════════════════════════════════════════════════════
    //  POST /api/auth/login
    // ════════════════════════════════════════════════════════════

    /**
     * LOGIN ENDPOINT
     *
     * URL:     POST http://localhost:8080/api/auth/login
     * Access:  Public (no token needed — you don't have one yet!)
     *
     * REQUEST BODY (JSON):
     * {
     *   "email":    "alice@example.com",
     *   "password": "mypassword123"
     * }
     *
     * SUCCESS RESPONSE (200 OK):
     * {
     *   "token":   "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NWYx...",
     *   "userId":  "65f1b2c3d4e5f6a7b8c9d0e1",
     *   "name":    "Alice Johnson",
     *   "email":   "alice@example.com"
     * }
     *
     * ERROR RESPONSES:
     * 400 Bad Request  → validation failed
     * 401 Unauthorized → wrong email or password
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════════════
    //  POST /api/auth/logout
    // ════════════════════════════════════════════════════════════

    /**
     * LOGOUT ENDPOINT
     *
     * URL:     POST http://localhost:8080/api/auth/logout
     * Auth:    Required (send JWT token in Authorization header)
     *
     * SUCCESS RESPONSE (200 OK):
     * { "success": true, "message": "Logged out successfully" }
     *
     * ── HOW JWT LOGOUT WORKS (Important concept!) ────────────────
     *
     * JWT tokens are STATELESS — the server does NOT store them.
     * So the server can't "delete" a token the way it can delete a session.
     *
     * ANALOGY: A concert wristband.
     * - Server GIVES you a wristband (JWT) at login
     * - Server does NOT keep a record of who has wristbands
     * - To "logout", you just CUT the wristband yourself (delete from localStorage)
     * - The wristband is technically still valid until it expires (30 min)
     *   but since you deleted it, you can't use it anymore
     *
     * For a college project, this simple approach is perfectly fine.
     *
     * ADVANCED APPROACH (for production apps):
     * Store invalidated tokens in a "blacklist" collection in MongoDB.
     * Check every incoming token against the blacklist.
     * This requires more setup but gives true instant logout.
     *
     * The JwtAuthFilter already validated the token before this method runs,
     * so we know it's a legitimate logged-in user making this request.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
}
