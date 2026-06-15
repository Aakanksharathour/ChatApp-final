package com.chatapp.repository;

import com.chatapp.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

/**
 * ════════════════════════════════════════════════════════════════
 *  UserRepository  —  THE GATEWAY to MongoDB
 * ════════════════════════════════════════════════════════════════
 *
 *  ANALOGY: If MongoDB is a FRIDGE, then UserRepository is the
 *  KITCHEN HELPER who knows how to put food in and take food out.
 *  You just ask: "Hey, get me the user with email X" and it handles
 *  all the MongoDB details for you automatically.
 *
 *  HOW IT WORKS:
 *  We EXTEND MongoRepository<User, String>:
 *  - User   = the class we're storing/retrieving (the document type)
 *  - String = the type of the @Id field in User (it's a String)
 *
 *  By extending MongoRepository, we GET FOR FREE (no code needed!):
 *  ✓ save(user)           → insert or update a user
 *  ✓ findById("abc123")   → get user by their MongoDB ID
 *  ✓ findAll()            → get all users
 *  ✓ deleteById("abc123") → delete a user
 *  ✓ existsById("abc123") → check if user exists
 *  ✓ count()              → how many users total
 *
 *  MAGIC METHOD NAMING:
 *  Spring Data MongoDB reads our method NAMES and automatically
 *  generates the database query. We don't write any SQL/MongoDB query!
 *
 *  Example:
 *  findByEmail("alice@example.com")
 *  → Spring sees "findBy" + "Email" → generates: { email: "alice@example.com" }
 *
 *  existsByEmail("alice@example.com")
 *  → generates: { email: "alice@example.com" } and returns true/false
 *
 *  @Repository → Marks this as a data-access component.
 *  Spring registers it and makes it available for injection elsewhere.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Find a user by their email address.
     *
     * Returns Optional<User> (not User directly) because the user
     * might NOT exist. Optional forces us to handle both cases:
     * 1. User found  → Optional.of(user)   → call .get()
     * 2. User missing → Optional.empty()   → call .orElseThrow() or .isPresent()
     *
     * Used in:
     * - Login: find the user and then check their password
     * - Register duplicate check: verify email isn't already taken
     *
     * Spring generates this MongoDB query automatically:
     * db.users.findOne({ email: "alice@example.com" })
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with this email already exists.
     * Returns true if found, false if not.
     *
     * Used in Registration to implement FR-2.6:
     * "prevent registration with an already-registered email address"
     *
     * Spring generates: db.users.countDocuments({ email: "..." }) > 0
     */
    boolean existsByEmail(String email);

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
}
