package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.checkers.logic.Board;
import com.checkers.logic.GameLogic;

@Component
// WebSocket handler for managing game connections and messages

public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameSessionManager sessionManager;

    private final ObjectMapper objectMapper;

    public GameWebSocketHandler(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.objectMapper = new ObjectMapper();
    }

// Sends a message to a specific WebSocket session
// Does nothing if the session is null or not open
// Logs an error if sending the message fails
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
// Broadcasts a message to all players in a specific game room
// Does nothing if the game room does not exist
// Logs an error if creating the WebSocket message fails
    private void broadcast(
            String gameId,
            String type,
            Integer player,
            String username,
            String message) {

        GameRoom room = sessionManager.getRoom(gameId);

        System.out.println(
            "BROADCAST CALLED: gameId=" + gameId
                + ", type=" + type
        );

        if (room == null) {
            System.err.println(
                "BROADCAST FAILED: room not found for " + gameId
            );
            return;
        }

        System.out.println(
            "BROADCAST ROOM: players=" + room.getPlayerCount()
                + ", status=" + room.getStatus()
                + ", player1=" + room.getPlayer1Username()
                + ", player2=" + room.getPlayer2Username()
        );

        WebSocketMessage webSocketMessage =
                new WebSocketMessage(
                        type,
                        gameId,
                        player,
                        room.getPlayerCount(),
                        room.getStatus().name(),
                        username,
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
        System.out.println("BROADCAST JSON: " + json);
        sessionManager.sendToGame(gameId, json);
    }

    // Called when a new WebSocket connection is established
    @Override
    // Handles the event when a WebSocket connection is established
    // Adds the session to the session manager
    // Retrieves the game ID and username from the session attributes
    // Sends an error message and closes the session if the username or game ID is missing
    // Sends an error message and closes the session if the game room does not exist
    // Attempts to join the player to the game room and sends an error message if joining fails
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        sessionManager.addSession(session);

        String gameId = getGameId(session);

        String username = (String) session.getAttributes().get("username");

        if (username == null) {

            sendMessage(
                    session,
                    new WebSocketMessage(
                            "ERROR",
                            null,
                            null,
                            null,
                            null,
                            null,
                            "You must be logged in to join a game."
                    )
            );

            session.close();
            return;
        }

        if (gameId == null) {
            sendMessage(
                    session,
                    new WebSocketMessage(
                            "ERROR",
                            null,
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
                            null,
                            "Game not found: " + gameId
                    )
            );
            session.close();
            return;
        }

        boolean reconnecting = room.hasDisconnectedPlayer(username);
        boolean joined = sessionManager.joinRoom(gameId, session, username);

        if (!joined) {
            sendMessage(
                    session,
                    new WebSocketMessage(
                            "ERROR",
                            gameId,
                            null,
                            null,
                            null,
                            null,
                            "Unable to join game. The game may be full."
                    )
            );
            session.close();
            return;
        }

        int playerNumber = room.getPlayerNumber(session);

        if (playerNumber == 0) {
            session.close();
            return;
        }

        sessionManager.cancelReconnectTimeout(gameId, playerNumber);

        sendMessage(
            session,
            new WebSocketMessage(
                    "CONNECTED",
                    gameId,
                    playerNumber,
                    room.getPlayerCount(),
                    room.getStatus().name(),
                    username,
                    "Connected to game."
            )
        );

        if (reconnecting) {
            broadcast(
                    gameId,
                    "PLAYER_RECONNECTED",
                    playerNumber,
                    username,
                    username + " reconnected to the game."
            );
        } else {
            broadcast(
                    gameId,
                    "PLAYER_JOINED",
                    playerNumber,
                    username,
                    username + " joined the game."
            );
        }

        if (room.getStatus() == GameRoom.Status.ACTIVE) {
            broadcast(
                    gameId,
                    "GAME_STARTED",
                    null,
                    null,
                    "Game is now active. Both players are connected."
            );
        }
    }

   
    
    @Override
    // Handles the event when a text message is received from a WebSocket session
    // Retrieves the game ID from the session attributes
    // Returns if the game ID is missing
    // Retrieves the game room from the session manager
    // Returns if the game room does not exist
    // Logs the received message
    // Broadcasts the message to all players in the game room
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage textMessage) throws Exception {

        String gameId = getGameId(session);
        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, 0, 0, "ERROR",
                    null, "Game room not found."
            ));
            return;
        }

        String username = (String) session.getAttributes().get("username");

        int playerNumber;
        if (username != null
                && username.equals(room.getPlayer1Username())) {
            playerNumber = 1;
        } else if (username != null
                && username.equals(room.getPlayer2Username())) {
            playerNumber = 2;
        } else {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, 0, room.getPlayerCount(),
                    room.getStatus().name(), username,
                    "You are not a player in this room."
            ));
            return;
        }

        if (room.getStatus() != GameRoom.Status.ACTIVE
        || !room.isPlayerConnected(1)
        || !room.isPlayerConnected(2)) {

            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, playerNumber,
                    room.getPlayerCount(), room.getStatus().name(),
                    username, "The game is paused while a player is disconnected."
            ));

            return;
        }

        JsonNode incoming = objectMapper.readTree(
                textMessage.getPayload()
        );

        String type = incoming.hasNonNull("type")
                ? incoming.get("type").asText()
                : "";

        if (!"MOVE".equals(type)) {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, playerNumber,
                    room.getPlayerCount(), room.getStatus().name(),
                    username, "Unsupported message type: " + type
            ));
            return;
        }

        if (room.getStatus() != GameRoom.Status.ACTIVE) {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, playerNumber,
                    room.getPlayerCount(), room.getStatus().name(),
                    username, "The game has not started."
            ));
            return;
        }

        JsonNode move = incoming.get("message");

        if (move == null || !move.isObject()) {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, playerNumber,
                    room.getPlayerCount(), room.getStatus().name(),
                    username, "Invalid move payload."
            ));
            return;
        }

        
        JsonNode fromRowNode = move.get("fromRow");
        JsonNode fromColNode = move.get("fromCol");
        JsonNode toRowNode = move.get("toRow");
        JsonNode toColNode = move.get("toCol");

        if (fromRowNode == null || !fromRowNode.canConvertToInt()
                || fromColNode == null || !fromColNode.canConvertToInt()
                || toRowNode == null || !toRowNode.canConvertToInt()
                || toColNode == null || !toColNode.canConvertToInt()) {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, playerNumber,
                    room.getPlayerCount(), room.getStatus().name(),
                    username, "Move coordinates are missing or invalid."
            ));
            return;
        }

        int fromRow = fromRowNode.asInt();
        int fromCol = fromColNode.asInt();
        int toRow = toRowNode.asInt();
        int toCol = toColNode.asInt();

        // Convert the 8x8 board coordinates into the 32 playable squares.
        int start = Board.toIndex(fromRow, fromCol);
        int end = Board.toIndex(toRow, toCol);

        if (start == -1 || end == -1) {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, playerNumber,
                    room.getPlayerCount(), room.getStatus().name(),
                    username, "Move uses an invalid or unplayable square."
            ));
            return;
        }

        GameLogic gameLogic = room.getGameLogic();
        boolean accepted;

        // Validate the turn and apply the move atomically for this room.
        synchronized (room) {
            int expectedPlayer = gameLogic.getCurrentPlayer()
                    == GameLogic.red_player ? 1 : 2;

            if (playerNumber != expectedPlayer) {
                sendMessage(session, new WebSocketMessage(
                        "ERROR", gameId, playerNumber,
                        room.getPlayerCount(), room.getStatus().name(),
                        username, "It is not your turn."
                ));
                return;
            }

            accepted = gameLogic.move(start, end);
        }

        if (!accepted) {
            sendMessage(session, new WebSocketMessage(
                    "ERROR", gameId, playerNumber,
                    room.getPlayerCount(), room.getStatus().name(),
                    username, "Illegal move. The move was rejected."
            ));
            return;
        }

        System.out.println(
                "SERVER ACCEPTED MOVE: game=" + gameId
                        + ", player=" + playerNumber
                        + ", " + fromRow + "," + fromCol
                        + " -> " + toRow + "," + toCol
        );

        // Only broadcast moves accepted by the server.
        broadcast(
                gameId,
                "MOVE",
                playerNumber,
                username,
                move.toString()
        );

    }



   @Override
    // Handles the event when a WebSocket connection is closed
    // Retrieves the game ID from the session attributes
    // Returns if the game ID is missing
    // Retrieves the game room from the session manager
    // Returns if the game room does not exist
   
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

        String username =
        playerNumber == 1
                ? room.getPlayer1Username()
                : room.getPlayer2Username();

        sessionManager.removePlayerFromGame(gameId, session);
        sessionManager.removeSession(session);

        GameRoom updatedRoom = sessionManager.getRoom(gameId);

        if (updatedRoom != null
                && updatedRoom.getStatus() != GameRoom.Status.FINISHED) {

            broadcast(
                    gameId,
                    "PLAYER_DISCONNECTED",
                    playerNumber,
                    username,
                    username + " disconnected. Waiting for reconnection."
            );

            broadcast(
                    gameId,
                    "GAME_WAITING",
                    null,
                    null,
                    "A player disconnected. The match is temporarily paused."
            );
        }

            
    }

    //simple getters for broadcast message system
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

    // Retrieves the game ID from the WebSocket session's URI query parameters
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