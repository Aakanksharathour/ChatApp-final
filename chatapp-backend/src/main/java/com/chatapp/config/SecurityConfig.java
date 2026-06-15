package com.chatapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════
 *  SecurityConfig  —  THE SECURITY RULEBOOK
 * ════════════════════════════════════════════════════════════════
 *
 *  This class defines:
 *  1. Which endpoints are PUBLIC  (no token needed)
 *  2. Which endpoints are PRIVATE (token required)
 *  3. How to validate the JWT token (via JwtAuthFilter)
 *  4. CORS rules (allow frontend on localhost:5173)
 *
 *  WHAT CHANGED FROM BEFORE?
 *  We added JwtAuthFilter — the filter that reads the JWT token
 *  from the Authorization header on every request.
 *
 *  Previously, protected routes would just block everyone with 401.
 *  Now, the JwtAuthFilter reads the token, validates it, and tells
 *  Spring Security "this user is authenticated" — so they get through.
 *
 *  FILTER ORDER MATTERS:
 *  addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
 *  → Our JWT filter runs BEFORE Spring's default login filter.
 *  → So JWT auth happens first on every request.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ── INJECT JwtAuthFilter ──────────────────────────────────────────────
    // Spring creates JwtAuthFilter (@Component) and injects it here.
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── 1. CSRF — disabled (we use JWT, not cookies) ──────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── 2. CORS — allow React frontend ────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── 3. SESSION — stateless (JWT handles sessions) ─────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── 4. AUTHORIZATION RULES ────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // PUBLIC: only register and login — no token needed
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                // WebSocket handshake — auth is handled by WebSocketAuthInterceptor (token in query param)
                // Spring Security can't intercept the WS upgrade the same way, so we permit here
                .requestMatchers("/ws/**").permitAll()

                // Uploaded files are served as static resources — no auth needed to view
                .requestMatchers("/uploads/**").permitAll()

                // EVERYTHING ELSE requires a valid JWT token
                .anyRequest().authenticated()
            )

            // ── 5. ADD JWT FILTER ─────────────────────────────────────
            // Run our JwtAuthFilter BEFORE Spring's default login filter.
            // This is how Spring Security learns about our JWT authentication.
            //
            // ANALOGY: Before the hotel room key reader (UsernamePasswordAuthFilter),
            // we add our own wristband scanner (JwtAuthFilter).
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS CONFIGURATION
     * Allows our React frontend (localhost:5173) to call our backend (localhost:8080).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
