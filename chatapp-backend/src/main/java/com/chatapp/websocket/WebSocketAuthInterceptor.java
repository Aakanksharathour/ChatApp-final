package com.chatapp.websocket;

import com.chatapp.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════
 *  WebSocketAuthInterceptor  —  The BOUNCER at the WebSocket door
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT IS AN INTERCEPTOR?
 *  ─────────────────────────────────────────────────────────────
 *  An interceptor runs BEFORE the WebSocket connection is established.
 *  It's like a bouncer at a club — it checks your ID before letting you in.
 *
 *  WHY DO WE NEED THIS?
 *  ─────────────────────────────────────────────────────────────
 *  Normal HTTP requests send the JWT token in a HEADER:
 *    Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
 *
 *  But WebSocket connections work differently. The browser's WebSocket API
 *  does NOT allow you to send custom headers during the initial handshake.
 *
 *  So instead, the token is sent as a QUERY PARAMETER:
 *    ws://localhost:8080/ws/chat?token=eyJhbGciOiJIUzI1NiIs...
 *
 *  This interceptor:
 *  1. Reads the token from the URL query parameter
 *  2. Validates it using JwtUtil
 *  3. If valid → extracts userId and stores it in the session attributes
 *  4. If invalid → rejects the connection (returns 401)
 *
 *  The userId stored in attributes is then available in ChatWebSocketHandler
 *  via: session.getAttributes().get("userId")
 *
 *  ANALOGY:
 *  ─────────────────────────────────────────────────────────────
 *  Think of WebSocket as a phone call:
 *  - HTTP = sending a letter (one request, one response, done)
 *  - WebSocket = phone call (connection stays open, both sides can talk anytime)
 *
 *  This interceptor = verifying the caller's identity before picking up.
 */
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Called BEFORE the WebSocket handshake.
     * Return true  → allow the connection
     * Return false → reject the connection
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        URI uri = request.getURI();
        String query = uri.getQuery();  // Everything after '?' in the URL

        // URL example: /ws/chat?token=eyJhbGciOiJIUzI1NiIs...
        // query = "token=eyJhbGciOiJIUzI1NiIs..."

        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6);  // Remove "token=" prefix

                    if (jwtUtil.isTokenValid(token)) {
                        String userId = jwtUtil.extractUserId(token);
                        // ✅ Store userId in session attributes — available throughout the connection
                        attributes.put("userId", userId);
                        log.debug("WebSocket auth success for userId={}", userId);
                        return true;  // Let them in!
                    }
                    break;
                }
            }
        }

        // ❌ Token missing or invalid — reject connection
        log.warn("WebSocket auth failed — no valid token in query params");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    /**
     * Called AFTER the handshake. We don't need to do anything here.
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
