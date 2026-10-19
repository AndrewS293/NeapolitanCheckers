package com.checkers.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
// Configuration class for setting up WebSocket handlers in the application
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;

    public WebSocketConfig(
            GameWebSocketHandler gameWebSocketHandler) {

        this.gameWebSocketHandler =
                gameWebSocketHandler;
    }

    @Override
    // Registers the WebSocket handlers with the registry
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry) {

        registry.addHandler(
                        gameWebSocketHandler,
                        "/online"
                )
                .addInterceptors(
                        new HttpSessionHandshakeInterceptor()
                )
                .setAllowedOriginPatterns("*");
    }
}