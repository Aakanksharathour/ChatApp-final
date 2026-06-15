package com.chatapp.config;

import com.chatapp.util.JwtUtil;
import com.chatapp.websocket.ChatWebSocketHandler;
import com.chatapp.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * ════════════════════════════════════════════════════════════════
 *  WebSocketConfig  —  Registers the WebSocket endpoint
 * ════════════════════════════════════════════════════════════════
 *
 *  This class is the "registration desk" — it tells Spring:
 *  "When a client connects to /ws/chat, hand them over to ChatWebSocketHandler."
 *
 *  TWO ANNOTATIONS EXPLAINED:
 *  ─────────────────────────────────────────────────────────────
 *  @Configuration  → This class provides Spring configuration (like @Bean methods)
 *  @EnableWebSocket → Activates Spring's WebSocket support (without this, /ws/chat doesn't work)
 *
 *  WHAT GETS REGISTERED:
 *  ─────────────────────────────────────────────────────────────
 *  Path:        /ws/chat
 *  Handler:     ChatWebSocketHandler (handles messages after connection)
 *  Interceptor: WebSocketAuthInterceptor (validates JWT before connection)
 *  Origins:     http://localhost:5173 (our React frontend)
 *
 *  HOW A WebSocket CONNECTION FLOWS:
 *  ─────────────────────────────────────────────────────────────
 *
 *  Browser                     Server
 *    │                            │
 *    │  GET /ws/chat?token=eyJ..  │
 *    │ ─────────────────────────► │  HTTP Upgrade Request
 *    │                            │  ↓
 *    │                    WebSocketAuthInterceptor.beforeHandshake()
 *    │                            │  → validates token
 *    │                            │  → stores userId in session attributes
 *    │                            │  → returns true (allow)
 *    │                            │
 *    │  101 Switching Protocols   │
 *    │ ◄───────────────────────── │  Connection upgraded to WebSocket!
 *    │                            │
 *    │                    ChatWebSocketHandler.afterConnectionEstablished()
 *    │                            │  → stores session in sessions map
 *    │                            │
 *    │  { type: "message", ... }  │
 *    │ ─────────────────────────► │  Real-time message
 *    │                    ChatWebSocketHandler.handleTextMessage()
 *    │                            │  → routes to recipient
 *    │                            │
 *    │  { type: "new_message" }   │
 *    │ ◄───────────────────────── │  Server pushes to recipient
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtUtil jwtUtil;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, JwtUtil jwtUtil) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(new WebSocketAuthInterceptor(jwtUtil))
                .setAllowedOrigins("http://localhost:5173");
    }
}
