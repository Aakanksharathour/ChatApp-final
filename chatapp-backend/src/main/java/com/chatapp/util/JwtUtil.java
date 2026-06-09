package com.chatapp.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * ════════════════════════════════════════════════════════════════
 *  JwtUtil  —  THE TOKEN FACTORY
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT IS JWT? (JSON Web Token)
 *  ─────────────────────────────
 *  Imagine you go to a concert. The guard checks your ticket once
 *  at the gate and gives you a wristband.
 *  For the rest of the night, any staff member can SEE your wristband
 *  and know you're a valid attendee — WITHOUT calling the gate every time.
 *
 *  JWT works the same way:
 *  1. You log in with email + password  →  server verifies you
 *  2. Server gives you a JWT "wristband" (a long string token)
 *  3. For every future API call, you send that token
 *  4. Server reads the token, verifies it, and knows who you are
 *     WITHOUT hitting the database again
 *
 *  A JWT has 3 parts separated by dots:
 *  eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VySWQifQ.signature
 *  ─────────────────────.──────────────────────.─────────
 *       HEADER               PAYLOAD             SIGNATURE
 *   (algorithm used)    (data we stored)      (security stamp)
 *
 *  The PAYLOAD typically contains:
 *  - sub (subject) = userId
 *  - email, name   = user info
 *  - exp           = expiry time
 *  - iat           = issued-at time
 *
 *  The SIGNATURE is created using our secret key — if anyone tampers
 *  with the token, the signature won't match and we reject it.
 *
 * ── @Component ──────────────────────────────────────────────────
 *  @Component → registers this class with Spring so other classes
 *  can use it via @Autowired injection.
 *  Think of it as telling Spring: "Hey, I exist, please manage me."
 */
@Component
public class JwtUtil {

    /**
     * @Value("${jwt.secret}") → reads the 'jwt.secret' value from
     * application.properties and injects it here automatically.
     * No hardcoding — easier to change per environment.
     */
    @Value("${jwt.secret}")
    private String secretKeyString;

    /**
     * @Value("${jwt.expiration}") → reads jwt.expiration from properties.
     * Default: 1800000 ms = 30 minutes (as per SRS NFR 4.2)
     */
    @Value("${jwt.expiration}")
    private long expirationMs;

    /**
     * Converts our Base64 secret string into a cryptographic SecretKey
     * that the JWT library can use for signing.
     *
     * Think of this as converting a "password" into a "stamp" that can
     * be applied to documents.
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * GENERATE TOKEN
     * ─────────────────────────────────────────────────────────────
     * Creates a brand-new JWT token for a user after successful login.
     *
     * @param userId  the user's MongoDB _id
     * @param email   the user's email
     * @param name    the user's display name
     * @return        a JWT string like "eyJhbGciOiJIUzI1NiJ9..."
     *
     * The token stores:
     * - subject = userId (who this token belongs to)
     * - email, name = extra user info (claims)
     * - issuedAt = NOW
     * - expiration = NOW + 30 minutes
     */
    public String generateToken(String userId, String email, String name) {
        return Jwts.builder()
                .subject(userId)                    // who this token is FOR
                .claim("email", email)              // extra data we pack inside
                .claim("name", name)
                .issuedAt(new Date())               // token created NOW
                .expiration(new Date(              // expires after 30 minutes
                        System.currentTimeMillis() + expirationMs))
                .signWith(getSignKey())             // sign it with our secret
                .compact();                         // build the final string
    }

    /**
     * EXTRACT ALL CLAIMS FROM TOKEN
     * ─────────────────────────────────────────────────────────────
     * "Claims" = the data packed inside the token (userId, email, name, expiry, etc.)
     *
     * This method:
     * 1. Parses the token string
     * 2. Verifies the signature (using our secret key)
     * 3. Returns all the data inside
     *
     * If the token was tampered with or expired, this throws an exception.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())   // verify signature
                .build()
                .parseSignedClaims(token)  // parse and validate
                .getPayload();             // get the data inside
    }

    /**
     * EXTRACT USER ID
     * The subject of the token = userId we set when generating.
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * EXTRACT EMAIL from the token's claims.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * IS TOKEN EXPIRED?
     * Compares the token's expiration date with the current time.
     * Returns true if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * IS TOKEN VALID?
     * A token is valid if:
     * 1. The signature matches (not tampered)
     * 2. It has NOT expired
     *
     * We can add more checks here in the future (e.g., check if user still exists).
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Token was malformed, signature didn't match, or other error
            return false;
        }
    }
}
