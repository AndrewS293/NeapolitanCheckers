package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);

        System.out.println(
                "Client connected: " + session.getId()
        );

        System.out.println(
                "Active connections: " + sessions.size()
        );
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());

        System.out.println(
                "Client disconnected: " + session.getId()
        );

        System.out.println(
                "Active connections: " + sessions.size()
        );
    }

    public void broadcast(String message) {

        for (WebSocketSession session : sessions.values()) {

            if (session.isOpen()) {
                try {
                    session.sendMessage(
                            new org.springframework.web.socket.TextMessage(message)
                    );
                } catch (IOException e) {
                    System.err.println(
                            "Failed to send message to "
                                    + session.getId()
                    );
                }
            }
        }
    }

    public int getConnectionCount() {
        return sessions.size();
    }
}