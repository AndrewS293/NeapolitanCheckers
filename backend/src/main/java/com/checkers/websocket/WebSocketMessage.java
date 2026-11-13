package com.checkers.websocket;

// implements the structure of messages sent over WebSocket connections
// Includes both the public room code and its database game ID.
public class WebSocketMessage {

    private String type;
    private String gameId;
    private Integer player;
    private Integer players;
    private String status;
    private String message;
    private String username;
    private Long databaseGameId;

    public WebSocketMessage() {
    }

    public WebSocketMessage(
            String type,
            String gameId,
            Integer player,
            Integer players,
            String status,
            String username,
            String message) {
        this(type, gameId, player, players, status, username, message, null);
    }

    public WebSocketMessage(
            String type,
            String gameId,
            Integer player,
            Integer players,
            String status,
            String username,
            String message,
            Long databaseGameId) {

        this.type = type;
        this.gameId = gameId;
        this.player = player;
        this.players = players;
        this.status = status;
        this.message = message;
        this.username = username;
        this.databaseGameId = databaseGameId;
    }

    // return the type of the WebSocket message
    public String getType() {
        return type;
    }

    // return the username of the player associated with the WebSocket message
    public String getUsername() {
        return username;
    }

    // return the game ID associated with the WebSocket message
    public String getGameId() {
        return gameId;
    }

    // return the player number associated with the WebSocket message
    public Integer getPlayer() {
        return player;
    }

    // return the number of players associated with the WebSocket message
    public Integer getPlayers() {
        return players;
    }

    // return the status associated with the WebSocket message
    public String getStatus() {
        return status;
    }

    // return the message associated with the WebSocket message
    public String getMessage() {
        return message;
    }

    public Long getDatabaseGameId() {
        return databaseGameId;
    }
}