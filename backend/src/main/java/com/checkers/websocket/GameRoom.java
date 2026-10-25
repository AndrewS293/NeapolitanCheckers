
package com.checkers.websocket;

import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ConcurrentHashMap;
import com.checkers.logic.*;

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
    private final long databaseGameId;
    private final GameVisibility visibility;
    private final String joinCode;

    private WebSocketSession player1;
    private WebSocketSession player2;

    // Keep usernames reserved during temporary disconnections.
    private String player1Username;
    private String player2Username;

    private Status status = Status.WAITING;

    // Each room keeps its own board and turn state.
    private GameLogic gameLogic = new GameLogic();

// Counts moves successfully saved to the database.
    private int savedMoveCount = 0;

    public GameRoom(
            String gameId,
            long databaseGameId,
            GameVisibility visibility,
            String joinCode) {
        this.gameId = gameId;
        this.databaseGameId = databaseGameId;
        this.visibility = visibility;
        this.joinCode = joinCode;
    }
    // get the ID of the game room
    // returns the game ID as a string
    public String getGameId() {
        return gameId;
    }
    // get the visibility of the game room

    public long getDatabaseGameId() {
        return databaseGameId;
    }

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


    public synchronized GameLogic getGameLogic() {
        return gameLogic;
    }
    
    public synchronized int getSavedMoveCount() {
    return savedMoveCount;
}

// Call only after the database confirms that the move was saved.
public synchronized void commitSavedMove(GameLogic validatedGame) {
    gameLogic = validatedGame;
    savedMoveCount++;
}

    public synchronized String getPlayer1Username() {
        return player1Username;
    }

    public synchronized String getPlayer2Username() {
        return player2Username;
    }

    public synchronized boolean addPlayer(
            WebSocketSession session,
            String username) {

        if (status == Status.FINISHED
                || session == null
                || username == null) {
            return false;
        }

        // Reclaim the original player 1 slot.
        if (username.equals(player1Username)) {
            if (player1 != null) {
                return false;
            }

            player1 = session;
            updateStatus();
            return true;
        }

        // Reclaim the original player 2 slot.
        if (username.equals(player2Username)) {
            if (player2 != null) {
                return false;
            }

            player2 = session;
            updateStatus();
            return true;
        }

        // Assign a genuinely unreserved slot.
        if (player1Username == null) {
            player1 = session;
            player1Username = username;
            updateStatus();
            return true;
        }

        if (player2Username == null) {
            player2 = session;
            player2Username = username;
            updateStatus();
            return true;
        }

        return false;
    }

    /**
     * Disconnect the session without releasing its player slot.
     */
    public synchronized int disconnectPlayer(
            WebSocketSession session) {

        if (session == null) {
            return 0;
        }

        if (player1 != null
                && player1.getId().equals(session.getId())) {
            player1 = null;
            updateStatus();
            return 1;
        }

        if (player2 != null
                && player2.getId().equals(session.getId())) {
            player2 = null;
            updateStatus();
            return 2;
        }

        return 0;
    }

    public synchronized boolean hasDisconnectedPlayer(
            String username) {

        return username != null
                && ((username.equals(player1Username)
                        && player1 == null)
                    || (username.equals(player2Username)
                        && player2 == null));
    }

    public synchronized boolean isPlayerConnected(int playerNumber) {
        if (playerNumber == 1) {
            return player1 != null && player1.isOpen();
        }

        if (playerNumber == 2) {
            return player2 != null && player2.isOpen();
        }

        return false;
    }

    public synchronized String getUsername(int playerNumber) {
        if (playerNumber == 1) {
            return player1Username;
        }

        if (playerNumber == 2) {
            return player2Username;
        }

        return null;
    }

    public synchronized boolean isFull() {
        // A disconnected player's slot remains reserved.
        return player1Username != null
                && player2Username != null;
    }

    public synchronized boolean isEmpty() {
        return player1Username == null
                && player2Username == null;
    }

    public synchronized int getPlayerCount() {
        int count = 0;

        if (player1 != null && player1.isOpen()) {
            count++;
        }

        if (player2 != null && player2.isOpen()) {
            count++;
        }

        return count;
    }

    public synchronized int getPlayerNumber(
            WebSocketSession session) {

        if (session == null) {
            return 0;
        }

        if (player1 != null
                && player1.getId().equals(session.getId())) {
            return 1;
        }

        if (player2 != null
                && player2.getId().equals(session.getId())) {
            return 2;
        }

        return 0;
    }

    public synchronized int getPlayerNumberByUsername(
            String username) {

        if (username != null
                && username.equals(player1Username)) {
            return 1;
        }

        if (username != null
                && username.equals(player2Username)) {
            return 2;
        }

        return 0;
    }

    public synchronized void finish() {
        status = Status.FINISHED;
    }

    private void updateStatus() {
        if (status == Status.FINISHED) {
            return;
        }

        if (player1 != null && player1.isOpen()
                && player2 != null && player2.isOpen()) {
            status = Status.ACTIVE;
        } else {
            status = Status.WAITING;
        }
    }
}
