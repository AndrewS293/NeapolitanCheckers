package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
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
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        sessionManager.addSession(session);

        String gameId = getGameId(session);

        if (gameId == null) {
            session.sendMessage(
                    new TextMessage("Connected, but no game ID was provided.")
            );
            session.close();
            return;
        }

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            session.sendMessage(
                    new TextMessage("Game not found: " + gameId)
            );
            session.close();
            return;
        }

        boolean joined = sessionManager.joinRoom(gameId, session);

        if (!joined) {
            session.sendMessage(
                    new TextMessage("Unable to join game. The game may be full.")
            );
            session.close();
            return;
        }

        int playerNumber = room.getPlayerCount();

        session.sendMessage(
                new TextMessage(
                        "Connected to game: " + gameId
                                + " as Player " + playerNumber
                )
        );

        sessionManager.sendToGame(
                gameId,
                "Player " + playerNumber + " joined the game."
        );

        if (room.getStatus() == GameRoom.Status.ACTIVE) {
            sessionManager.sendToGame(
                    gameId,
                    "Game is now ACTIVE. Both players are connected."
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

        System.out.println(
                "Game " + gameId
                        + " | Client "
                        + session.getId()
                        + ": "
                        + message.getPayload()
        );

        String response =
                "Game " + gameId
                        + " | Client "
                        + session.getId()
                        + ": "
                        + message.getPayload();

        sessionManager.sendToGame(gameId, response);
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

            sessionManager.sendToGame(
                    gameId,
                    "Player " + playerNumber
                            + " left the game."
            );

            sessionManager.sendToGame(
                    gameId,
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