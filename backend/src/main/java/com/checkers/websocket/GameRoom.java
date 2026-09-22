package com.checkers.websocket;

import org.springframework.web.socket.WebSocketSession;

public class GameRoom {

    public enum Status {
        WAITING,
        ACTIVE,
        FINISHED
    }

    private final String gameId;

    private final GameVisibility visibility;

    private final String joinCode;

    private WebSocketSession player1;
    private WebSocketSession player2;

    private String player1Username;
    private String player2Username;

    private Status status = Status.WAITING;

    public GameRoom(
            String gameId,
            GameVisibility visibility,
            String joinCode) {

        this.gameId = gameId;
        this.visibility = visibility;
        this.joinCode = joinCode;
    }

    public String getGameId() {
        return gameId;
    }

    public GameVisibility getVisibility() {
        return visibility;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public WebSocketSession getPlayer1() {
        return player1;
    }

    public WebSocketSession getPlayer2() {
        return player2;
    }

    public Status getStatus() {
        return status;
    }

    public boolean addPlayer(WebSocketSession session, String username) {

        if (status == Status.FINISHED) {
            return false;
        }

        if (player1 == null) {
            player1 = session;
            player1Username = username;
            updateStatus();
            return true;
        }

        if (player2 == null) {
            player2 = session;
            player2Username = username;
            updateStatus();
            return true;
        }

        return false;
    }

    public void removePlayer(WebSocketSession session) {

        if (player1 != null &&
                player1.getId().equals(session.getId())) {
            player1 = null;
            player1Username = null;
        }

        if (player2 != null &&
                player2.getId().equals(session.getId())) {
            player2 = null;
            player2Username = null;
        }

        updateStatus();
    }

    private void updateStatus() {

        if (player1 != null && player2 != null) {
            status = Status.ACTIVE;
        } else {
            status = Status.WAITING;
        }
    }

    public boolean isFull() {

        return player1 != null &&
                player2 != null;
    }

    public boolean isEmpty() {

        return player1 == null &&
                player2 == null;
    }

    public int getPlayerCount() {

        int count = 0;

        if (player1 != null) {
            count++;
        }

        if (player2 != null) {
            count++;
        }

        return count;
    }

    public String getPlayer1Username() {
        return player1Username;
    }

    public String getPlayer2Username() {
        return player2Username;
    }
}