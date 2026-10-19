package com.checkers.websocket;

import org.springframework.web.socket.WebSocketSession;
// represents a game room for the application
// manages the players, game status, and visibility settings
// provides methods for adding and removing players, checking game status, and retrieving player information
// ensures that the game room is properly managed and that players cannot join or leave inappropriately
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
    // get the ID of the game room
    // returns the game ID as a string
    public String getGameId() {
        return gameId;
    }
    // get the visibility of the game room
    public GameVisibility getVisibility() {
        return visibility;
    }
    // get the join code of the game room
    public String getJoinCode() {
        return joinCode;
    }
    // get the first player of the game room
    public WebSocketSession getPlayer1() {
        return player1;
    }
    // get the second player of the game room
    public WebSocketSession getPlayer2() {
        return player2;
    }

    // get the status of the game room
    // returns the current status of the game room as a Status enum value
    public Status getStatus() {
        return status;
    }

    // add a player to the game room
    // returns true if the player was successfully added, false otherwise
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

    // remove a player from the game room
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

    // check if the game room is full
    // returns true if the game room is full, false otherwise
    public boolean isFull() {

        return player1 != null &&
                player2 != null;
    }

    // check if the game room is empty
    // returns true if the game room is empty, false otherwise
    public boolean isEmpty() {

        return player1 == null &&
                player2 == null;
    }

    // get the number of players in the game room
    // returns the number of players as an integer
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

    // get the username of the first player
    // returns the username as a string
    public String getPlayer1Username() {
        return player1Username;
    }

    // get the username of the second player
    // returns the username as a string
    public String getPlayer2Username() {
        return player2Username;
    }
}