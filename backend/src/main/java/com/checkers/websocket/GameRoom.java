
package com.checkers.websocket;

import org.springframework.web.socket.WebSocketSession;
<<<<<<< HEAD
// represents a game room for the application
// manages the players, game status, and visibility settings
// provides methods for adding and removing players, checking game status, and retrieving player information
// ensures that the game room is properly managed and that players cannot join or leave inappropriately
=======
import com.checkers.logic.GameLogic;

>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b
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
    private final GameLogic gameLogic = new GameLogic();

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
<<<<<<< HEAD
    // get the visibility of the game room
=======

    public long getDatabaseGameId() {
        return databaseGameId;
    }

>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b
    public GameVisibility getVisibility() {
        return visibility;
    }
    // get the join code of the game room
    public String getJoinCode() {
        return joinCode;
    }
<<<<<<< HEAD
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

<<<<<<< HEAD
    // add a player to the game room
    // returns true if the player was successfully added, false otherwise
    public boolean addPlayer(WebSocketSession session, String username) {
=======
=======

    public synchronized WebSocketSession getPlayer1() {
        return player1;
    }

    public synchronized WebSocketSession getPlayer2() {
        return player2;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized GameLogic getGameLogic() {
        return gameLogic;
    }
>>>>>>> c64f96214cc677383dc7dea08ba4818d633b2e37

    public synchronized String getPlayer1Username() {
        return player1Username;
    }

    public synchronized String getPlayer2Username() {
        return player2Username;
    }

    public synchronized boolean addPlayer(
            WebSocketSession session,
            String username) {
>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b

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

<<<<<<< HEAD
    // remove a player from the game room
    public void removePlayer(WebSocketSession session) {
=======
    /**
     * Disconnect the session without releasing its player slot.
     */
    public synchronized int disconnectPlayer(
            WebSocketSession session) {
>>>>>>> c64f96214cc677383dc7dea08ba4818d633b2e37

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

<<<<<<< HEAD
    // check if the game room is full
    // returns true if the game room is full, false otherwise
    public boolean isFull() {
=======
    public synchronized String getUsername(int playerNumber) {
        if (playerNumber == 1) {
            return player1Username;
        }
>>>>>>> c64f96214cc677383dc7dea08ba4818d633b2e37

        if (playerNumber == 2) {
            return player2Username;
        }

        return null;
    }

<<<<<<< HEAD
    // check if the game room is empty
    // returns true if the game room is empty, false otherwise
    public boolean isEmpty() {

        return player1 == null &&
                player2 == null;
    }

    // get the number of players in the game room
    // returns the number of players as an integer
    public int getPlayerCount() {
=======
    public synchronized boolean isFull() {
        // A disconnected player's slot remains reserved.
        return player1Username != null
                && player2Username != null;
    }

    public synchronized boolean isEmpty() {
        return player1Username == null
                && player2Username == null;
    }
>>>>>>> c64f96214cc677383dc7dea08ba4818d633b2e37

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

<<<<<<< HEAD
    // get the username of the first player
    // returns the username as a string
    public String getPlayer1Username() {
        return player1Username;
    }

    // get the username of the second player
    // returns the username as a string
    public String getPlayer2Username() {
        return player2Username;
=======
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
>>>>>>> c64f96214cc677383dc7dea08ba4818d633b2e37
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
