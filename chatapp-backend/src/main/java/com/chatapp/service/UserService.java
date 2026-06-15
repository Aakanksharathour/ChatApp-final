package com.chatapp.service;

import com.chatapp.dto.UpdateProfileRequest;
import com.chatapp.dto.UserProfileResponse;
import com.chatapp.exception.ApiException;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ════════════════════════════════════════════════════════════════
 *  UserService  —  BUSINESS LOGIC for User / Profile APIs
 * ════════════════════════════════════════════════════════════════
 *
 *  Handles two operations:
 *  1. Get a user's profile by ID      → GET /api/user/:id
 *  2. Update a user's profile         → PUT /api/user/:id
 *
 *  KEY SECURITY RULE:
 *  Only the owner can update their own profile.
 *  If user A tries to update user B's profile → 403 Forbidden.
 *  We enforce this by comparing the ID from the JWT token
 *  (requesterId) with the ID in the URL (targetId).
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ════════════════════════════════════════════════════════════
    //  GET USER BY ID
    // ════════════════════════════════════════════════════════════

    /**
     * Fetches a user's profile by their MongoDB ID.
     *
     * @param id  the user's MongoDB _id (from the URL path)
     * @return    UserProfileResponse (safe version — no password!)
     *
     * FLOW:
     * 1. Find user in MongoDB by ID
     * 2. If not found → throw 404
     * 3. Convert User → UserProfileResponse (excludes password)
     * 4. Return it
     */
    public UserProfileResponse getUserById(String id) {
        log.debug("Fetching profile for userId: {}", id);

        // findById() returns Optional<User>
        // .orElseThrow() → if empty (no user with this ID), throw 404
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "User not found.",
                        HttpStatus.NOT_FOUND   // 404
                ));

        log.debug("Found user: {}", user.getEmail());

        // Convert User → UserProfileResponse
        // We intentionally SKIP the password field here — never send it!
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfilePhoto()
        );
    }

    // ════════════════════════════════════════════════════════════
    //  UPDATE USER PROFILE
    // ════════════════════════════════════════════════════════════

    /**
     * Updates a user's name and/or profile photo.
     *
     * @param targetId    the user ID from the URL (/api/user/THIS_ID)
     * @param requesterId the user ID extracted from the JWT token
     *                    (tells us WHO is making the request)
     * @param request     the new name and/or profilePhoto to set
     * @return            updated UserProfileResponse
     *
     * FLOW:
     * 1. Security check: requesterId must equal targetId (only update yourself!)
     * 2. Find user in MongoDB
     * 3. Update only the fields that were provided
     * 4. Save back to MongoDB
     * 5. Return updated profile
     *
     * WHY TWO IDs?
     * targetId   = who the client WANTS to update (from URL)
     * requesterId = who the client ACTUALLY IS (from JWT token)
     *
     * If they don't match → someone is trying to update someone else's profile!
     * → Throw 403 Forbidden
     */
    public List<UserProfileResponse> searchUsers(String query, String requesterId) {
        log.debug("Searching users with query: {}", query);
        return userRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream()
                .filter(u -> !u.getId().equals(requesterId))
                .map(u -> new UserProfileResponse(u.getId(), u.getName(), u.getEmail(), u.getProfilePhoto()))
                .collect(Collectors.toList());
    }

    public UserProfileResponse updateProfile(String targetId, String requesterId, UpdateProfileRequest request) {
        log.debug("Update request: targetId={}, requesterId={}", targetId, requesterId);

        // ── SECURITY CHECK ────────────────────────────────────────────
        // The JWT token tells us WHO is logged in (requesterId).
        // The URL tells us WHICH profile to update (targetId).
        // They must be the same person!
        //
        // Example attack: User A sends PUT /api/user/USER_B_ID
        // Their JWT says requesterId = USER_A_ID
        // targetId = USER_B_ID  ≠  requesterId = USER_A_ID → BLOCKED!
        if (!targetId.equals(requesterId)) {
            throw new ApiException(
                    "You can only update your own profile.",
                    HttpStatus.FORBIDDEN   // 403
            );
        }

        // ── FIND THE USER ─────────────────────────────────────────────
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new ApiException(
                        "User not found.",
                        HttpStatus.NOT_FOUND   // 404
                ));

        // ── UPDATE ONLY PROVIDED FIELDS ───────────────────────────────
        // Both fields are optional. We only update what was sent.
        // If name is null or blank → don't change the existing name.
        // If profilePhoto is null  → don't change the existing photo.
        //
        // This is called a PARTIAL UPDATE — we don't overwrite everything,
        // only what the user explicitly chose to change.
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
            log.debug("Updating name to: {}", request.name().trim());
        }

        if (request.profilePhoto() != null) {
            user.setProfilePhoto(request.profilePhoto());
            log.debug("Updating profilePhoto");
        }

        // ── SAVE TO MONGODB ───────────────────────────────────────────
        // Since the user already has an _id, .save() does an UPDATE (not INSERT).
        User savedUser = userRepository.save(user);
        log.info("Profile updated for userId: {}", savedUser.getId());

        // ── RETURN UPDATED PROFILE ────────────────────────────────────
        return new UserProfileResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getProfilePhoto()
        );
    }
}
