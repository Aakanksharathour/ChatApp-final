package com.chatapp.controller;

import com.chatapp.dto.UpdateProfileRequest;
import com.chatapp.dto.UserProfileResponse;
import com.chatapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════
 *  UserController  —  Handles User / Profile API endpoints
 * ════════════════════════════════════════════════════════════════
 *
 *  Endpoints:
 *  GET  /api/user/:id  → fetch user profile
 *  PUT  /api/user/:id  → update user profile
 *
 *  Both require a valid JWT token in the Authorization header.
 *
 *  NEW CONCEPT: Authentication parameter
 *  ─────────────────────────────────────
 *  In the updateProfile method, you'll see:
 *      Authentication authentication
 *
 *  Spring automatically injects this object. It contains info
 *  about the CURRENTLY LOGGED IN USER, which our JwtAuthFilter
 *  put into the SecurityContext after validating the JWT token.
 *
 *  authentication.getName() → returns the userId we stored in the token
 *
 *  This is how we know WHO is making the request without asking them
 *  to send their userId in the request body — the JWT token already has it!
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ════════════════════════════════════════════════════════════
    //  GET /api/user/{id}
    // ════════════════════════════════════════════════════════════

    /**
     * GET USER PROFILE
     *
     * URL:     GET http://localhost:8080/api/user/{id}
     * Auth:    Required (send JWT token in Authorization header)
     *
     * Example: GET http://localhost:8080/api/user/6a1e24792a468463aee9be12
     *
     * SUCCESS RESPONSE (200 OK):
     * {
     *   "id":           "6a1e24792a468463aee9be12",
     *   "name":         "Alice Johnson",
     *   "email":        "alice@example.com",
     *   "profilePhoto": null
     * }
     *
     * ERROR:
     * 404 → user with this ID doesn't exist
     * 401 → no JWT token provided
     *
     * ── @PathVariable ────────────────────────────────────────────
     * @PathVariable String id
     * → Reads the {id} part from the URL.
     * → GET /api/user/abc123  →  id = "abc123"
     * → It's different from @RequestBody (which reads JSON body) and
     *   @RequestParam (which reads ?key=value query params).
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(Authentication authentication) {
        UserProfileResponse profile = userService.getUserById(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(
            @RequestParam String q,
            Authentication authentication) {
        List<UserProfileResponse> results = userService.searchUsers(q, authentication.getName());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUser(@PathVariable String id) {
        UserProfileResponse profile = userService.getUserById(id);
        return ResponseEntity.ok(profile);
    }

    // ════════════════════════════════════════════════════════════
    //  PUT /api/user/{id}
    // ════════════════════════════════════════════════════════════

    /**
     * UPDATE USER PROFILE
     *
     * URL:     PUT http://localhost:8080/api/user/{id}
     * Auth:    Required (JWT token must match the {id} being updated)
     *
     * REQUEST BODY (JSON):
     * {
     *   "name":         "Alice Updated",
     *   "profilePhoto": "https://cdn.example.com/photo.jpg"
     * }
     * Both fields are optional — send only what you want to change.
     *
     * SUCCESS RESPONSE (200 OK):
     * {
     *   "success": true,
     *   "message": "Profile updated successfully",
     *   "user": {
     *     "id":           "6a1e24792a468463aee9be12",
     *     "name":         "Alice Updated",
     *     "email":        "alice@example.com",
     *     "profilePhoto": "https://cdn.example.com/photo.jpg"
     *   }
     * }
     *
     * ERRORS:
     * 401 → no token
     * 403 → token belongs to a different user (can't update someone else's profile)
     * 404 → user not found
     *
     * ── Authentication parameter ─────────────────────────────────
     * Spring auto-injects this from SecurityContextHolder.
     * authentication.getName() = userId stored in JWT by JwtAuthFilter.
     * We pass this to the service as "requesterId" for the ownership check.
     */
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMe(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        UserProfileResponse updated = userService.updateProfile(userId, userId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated successfully");
        response.put("user", updated);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        // Get the logged-in user's ID from the JWT token
        // JwtAuthFilter already validated the token and put userId here
        String requesterId = authentication.getName();

        // Service handles the ownership check + update logic
        UserProfileResponse updated = userService.updateProfile(id, requesterId, request);

        // Build response matching the API docs format
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated successfully");
        response.put("user", updated);

        return ResponseEntity.ok(response);  // 200 OK
    }
}
