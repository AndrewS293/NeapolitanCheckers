package com.checkers.websocket;

public class WebSocketMessage {

    private String type;
    private String gameId;
    private Integer player;
    private Integer players;
    private String status;
    private String message;
    private String username;

    public WebSocketMessage() {
    }

    public WebSocketMessage(
            String type,
            String gameId,
            Integer player,
            Integer players,
            String status,
            String message,
            String username) {

        this.type = type;
        this.gameId = gameId;
        this.player = player;
        this.players = players;
        this.status = status;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getGameId() {
        return gameId;
    }

    public Integer getPlayer() {
        return player;
    }

    public Integer getPlayers() {
        return players;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}