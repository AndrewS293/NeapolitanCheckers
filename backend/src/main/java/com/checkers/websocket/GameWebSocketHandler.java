package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameSessionManager sessionManager;

    private final ObjectMapper objectMapper;

    public GameWebSocketHandler(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.objectMapper = new ObjectMapper();
    }


    private void sendMessage(
            WebSocketSession session,
            WebSocketMessage message) {

        try {
            String json = objectMapper.writeValueAsString(message);

            session.sendMessage(
                    new TextMessage(json)
            );

        } catch (Exception e) {

            System.err.println(
                    "Error sending message to client "
                            + session.getId()
                            + ": "
                            + e.getMessage()
            );
        }
    }

    private void broadcast(
            String gameId,
            String type,
            Integer player,
            String message) {

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            return;
        }

        WebSocketMessage webSocketMessage =
                new WebSocketMessage(
                        type,
                        gameId,
                        player,
                        room.getPlayerCount(),
                        room.getStatus().name(),
                        message
                );

        String json;

        try {
            json = objectMapper.writeValueAsString(
                    webSocketMessage
            );
        } catch (Exception e) {
            System.err.println(
                    "Failed to create WebSocket message: "
                            + e.getMessage()
            );
            return;
        }

        sessionManager.sendToGame(gameId, json);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        sessionManager.addSession(session);

        String gameId = getGameId(session);

        if (gameId == null) {
            sendMessage(
                    session,
                    new WebSocketMessage(
                            "ERROR",
                            null,
                            null,
                            null,
                            null,
                            "No game ID was provided."
                    )
            );
            session.close();
            return;
        }

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            sendMessage(
                    session,
                    new WebSocketMessage(
                            "ERROR",
                            gameId,
                            null,
                            null,
                            null,
                            "Game not found: " + gameId
                    )
            );
            session.close();
            return;
        }

        boolean joined = sessionManager.joinRoom(gameId, session);

        if (!joined) {
            sendMessage(
                    session,
                    new WebSocketMessage(
                            "ERROR",
                            gameId,
                            null,
                            null,
                            null,
                            "Unable to join game. The game may be full."
                    )
            );
            session.close();
            return;
        }

        int playerNumber = room.getPlayerCount();

        sendMessage(
            session,
            new WebSocketMessage(
                    "CONNECTED",
                    gameId,
                    playerNumber,
                    room.getPlayerCount(),
                    room.getStatus().name(),
                    "Connected to game."
            )
        );

        broadcast(
                gameId,
                "PLAYER_JOINED",
                playerNumber,
                "Player " + playerNumber + " joined the game."
        );

        if (room.getStatus() == GameRoom.Status.ACTIVE) {
            broadcast(
                    gameId,
                    "GAME_STARTED",
                    null,
                    "Game is now active. Both players are connected."
            );
        }
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) {

        String gameId = getGameId(session);

        if (gameId == null) {
            return;
        }

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            return;
        }

        System.out.println(
                "Game " + gameId
                        + " | Client "
                        + session.getId()
                        + ": "
                        + message.getPayload()
        );

        broadcast(
                gameId,
                "GAME_MESSAGE",
                getPlayerNumber(room, session),
                message.getPayload()
        );
    }

   @Override
public void afterConnectionClosed(
        WebSocketSession session,
        CloseStatus status) {

        String gameId = getGameId(session);

        if (gameId == null) {
            sessionManager.removeSession(session);
            return;
        }

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            sessionManager.removeSession(session);
            return;
        }

        int playerNumber = getPlayerNumber(room, session);

        
        if (playerNumber == 0) {
            sessionManager.removeSession(session);
            return;
        }

        sessionManager.removePlayerFromGame(
                gameId,
                session
        );

        sessionManager.removeSession(session);

        GameRoom updatedRoom =
                sessionManager.getRoom(gameId);

        
        if (updatedRoom != null &&
                !updatedRoom.isEmpty()) {

            broadcast(
                    gameId,
                    "PLAYER_LEFT",
                    playerNumber,
                    "Player " + playerNumber + " left the game."
            );

            broadcast(
                    gameId,
                    "GAME_WAITING",
                    null,
                    "Game is waiting for another player."
            );
        }
    }

    private int getPlayerNumber(
            GameRoom room,
            WebSocketSession session) {

        if (room == null) {
            return 0;
        }

        if (room.getPlayer1() != null &&
                room.getPlayer1().getId().equals(session.getId())) {
            return 1;
        }

        if (room.getPlayer2() != null &&
                room.getPlayer2().getId().equals(session.getId())) {
            return 2;
        }

        return 0;
    }

    private String getGameId(WebSocketSession session) {

        if (session.getUri() == null) {
            return null;
        }

        String query = session.getUri().getQuery();

        if (query == null) {
            return null;
        }

        for (String parameter : query.split("&")) {

            String[] parts = parameter.split("=");

            if (parts.length == 2 &&
                    parts[0].equals("gameId")) {

                return parts[1];
            }
        }

        return null;
    }
}