package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameSessionManager sessionManager;

    public GameWebSocketHandler(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) throws Exception {

        sessionManager.addSession(session);

        session.sendMessage(
                new TextMessage(
                        "Connected to Checkers server!"
                )
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) {

        System.out.println(
                "Received from "
                        + session.getId()
                        + ": "
                        + message.getPayload()
        );

        String response =
                "Client " + session.getId()
                        + " says: "
                        + message.getPayload();

        sessionManager.broadcast(response);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            org.springframework.web.socket.CloseStatus status) {

        sessionManager.removeSession(session);
    }
}