package com.chatapp.config;

import com.chatapp.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════
 *  JwtAuthFilter  —  THE SECURITY CHECKPOINT
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT IS A FILTER?
 *  ─────────────────
 *  A Filter runs on EVERY incoming HTTP request BEFORE it reaches
 *  the Controller. Think of it like a metal detector at an airport:
 *
 *  PASSENGER → [METAL DETECTOR] → GATE → FLIGHT
 *  REQUEST   → [JwtAuthFilter]  → CONTROLLER → RESPONSE
 *
 *  This filter specifically checks: "Does this request have a valid JWT token?"
 *
 *  ─────────────────────────────────────────────────────────────
 *  HOW IT WORKS — STEP BY STEP:
 *  ─────────────────────────────────────────────────────────────
 *
 *  Every protected API call (like GET /api/user/123) must include:
 *  Header: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 *
 *  Step 1: Read the "Authorization" header from the request
 *  Step 2: Check it starts with "Bearer "
 *  Step 3: Extract the token part (remove "Bearer " prefix)
 *  Step 4: Validate the token using JwtUtil
 *  Step 5: Extract userId from the token
 *  Step 6: Tell Spring Security: "This user is authenticated!"
 *  Step 7: Let the request continue to the Controller
 *
 *  If NO token or INVALID token:
 *  → Don't block it here (let SecurityConfig handle it)
 *  → SecurityConfig will return 401 Unauthorized
 *
 *  ─────────────────────────────────────────────────────────────
 *  WHY extends OncePerRequestFilter?
 *  ─────────────────────────────────────────────────────────────
 *  OncePerRequestFilter guarantees this filter runs EXACTLY ONCE
 *  per request, even if the request is forwarded internally.
 *  Without it, the filter could run multiple times for one request.
 *
 *  @Component → Spring manages this class. It gets auto-discovered
 *               and injected into SecurityConfig.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * This method runs for EVERY request.
     *
     * @param request     → the incoming HTTP request (we read headers from it)
     * @param response    → the outgoing HTTP response
     * @param filterChain → the chain of filters; we call filterChain.doFilter()
     *                      to pass the request to the NEXT filter or Controller
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // ── STEP 1: Read the Authorization header ────────────────────
        // Example value: "Bearer eyJhbGciOiJIUzI1NiJ9..."
        String authHeader = request.getHeader("Authorization");

        // ── STEP 2: Check if header exists and starts with "Bearer " ──
        // If not present (e.g., public routes like /api/auth/register),
        // just skip this filter and move on.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;  // stop processing this filter, move to next
        }

        // ── STEP 3: Extract the token ────────────────────────────────
        // authHeader = "Bearer eyJhbGciOiJIUzI1NiJ9..."
        // substring(7) removes the first 7 characters ("Bearer ")
        // token = "eyJhbGciOiJIUzI1NiJ9..."
        String token = authHeader.substring(7);

        // ── STEP 4 & 5: Validate token and extract userId ─────────────
        // isTokenValid() checks:
        //   1. Signature is correct (not tampered)
        //   2. Token has not expired
        if (jwtUtil.isTokenValid(token)) {

            // Extract the userId we stored in the token when the user logged in
            String userId = jwtUtil.extractUserId(token);

            // ── STEP 6: Tell Spring Security this user is authenticated ──
            //
            // UsernamePasswordAuthenticationToken is Spring's standard way
            // to represent an authenticated user.
            //
            // Parameters:
            // 1. principal   = userId (who is this person? → their ID)
            // 2. credentials = null   (we don't need password anymore — token was verified)
            // 3. authorities = empty list (roles/permissions — we'll add later)
            //
            // This is like saying: "I, the filter, confirm this user is valid.
            // Their ID is userId. Let them through."
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());

            // Attach extra request details (IP address, session info) — optional but good practice
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Put the authentication object into Spring Security's "context"
            // This is like putting a wristband on the user for this request.
            // Any Controller can now call SecurityContextHolder.getContext().getAuthentication()
            // to know WHO is making the request.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // ── STEP 7: Continue the request to the next filter / Controller ──
        // Whether the token was valid or not, we always call doFilter()
        // to pass the request along. If auth failed, SecurityConfig will
        // handle it and return 401.
        filterChain.doFilter(request, response);
    }
}
