package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
// Manages WebSocket sessions and game rooms for the checkers game
// Provides methods to add and remove sessions, create and retrieve game rooms,
// handle player joins and leaves, and send messages to players within a game room.
public class GameSessionManager {

    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private final Map<String, GameRoom> rooms =
            new ConcurrentHashMap<>();

    // add a WebSocket session to the manager
    // stores the session in the sessions map using its ID as the key
    public void addSession(WebSocketSession session) {

        sessions.put(session.getId(), session);

        System.out.println(
                "Client connected: " + session.getId()
        );

        System.out.println(
                "Active connections: " + sessions.size()
        );
    }

    // remove a WebSocket session from the manager
    // removes the session from the sessions map using its ID as the key
    public void removeSession(WebSocketSession session) {

        sessions.remove(session.getId());

        System.out.println(
                "Client disconnected: " + session.getId()
        );

        System.out.println(
                "Active connections: " + sessions.size()
        );
    }

    public GameRoom createRoom(
            String gameId,
            long databaseGameId,
            GameVisibility visibility,
            String joinCode) {

        GameRoom room = new GameRoom(
                gameId,
                databaseGameId,
                visibility,
                joinCode
        );

<<<<<<< HEAD
    // create a new game room
    // stores the room in the rooms map using its game ID as the key
    public GameRoom createRoom(String gameId, GameVisibility visibility, String joinCode) {
        GameRoom room = new GameRoom(gameId, visibility, joinCode);
=======
>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b
        rooms.put(gameId, room);

        System.out.println(
                "Created "
                        + visibility.name().toLowerCase()
                        + " game room: "
                        + gameId
                        + " | Database ID: "
                        + databaseGameId
        );

        return room;
    }

    // get a game room by its game ID
    // returns the game room if found, null otherwise
    public GameRoom getRoom(String gameId) {
        return rooms.get(gameId);
    }

<<<<<<< HEAD

    // get a game room by its join code
    // returns the game room if found, null otherwise
=======
>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b
    public GameRoom getRoomByJoinCode(String joinCode) {

        for (GameRoom room : rooms.values()) {

            if (joinCode != null &&
                    joinCode.equals(room.getJoinCode())) {

                return room;
            }
        }

        return null;
    }

    // get all open public games
    // returns a list of game rooms that are public, waiting, and not full
    public List<GameRoom> getOpenPublicGames() {

        List<GameRoom> openPublicGames =
                new ArrayList<>();

        for (GameRoom room : rooms.values()) {

            if (room.getVisibility() == GameVisibility.PUBLIC
                    && room.getStatus() == GameRoom.Status.WAITING
                    && !room.isFull()) {

                openPublicGames.add(room);
            }
        }

        return openPublicGames;
    }

<<<<<<< HEAD
    // allow a player to join a game room
    // returns true if the player successfully joined, false otherwise
    public boolean joinRoom(String gameId, WebSocketSession session, String username) {
=======
    public boolean joinRoom(
            String gameId,
            WebSocketSession session,
            String username) {

>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b
        GameRoom room = rooms.get(gameId);

        if (room == null) {
            return false;
        }

        if (room.isFull()) {
            return false;
        }

        boolean added =
                room.addPlayer(session, username);

        if (added) {

            System.out.println(
                    "Client "
                            + session.getId()
                            + " joined room: "
                            + gameId
            );
        }

        System.out.println(
                "Players in game: "
                        + room.getPlayerCount()
        );

        return added;
    }

    // allow a player to leave a game room
    // removes the player from the game room and updates the room status
    public void removePlayerFromGame(
            String gameId,
            WebSocketSession session) {

        GameRoom room = rooms.get(gameId);

        if (room == null) {
            return;
        }

        room.removePlayer(session);

        System.out.println(
                "Client "
                        + session.getId()
                        + " left game "
                        + gameId
        );

        System.out.println(
                "Players in game: "
                        + room.getPlayerCount()
        );

        if (room.isEmpty()) {

            rooms.remove(gameId);

            System.out.println(
                    "Removed empty game room: "
                            + gameId
            );
        }
    }

<<<<<<< HEAD
    // send a message to all players in a game room
    // does nothing if the game room does not exist
    public void sendToGame(String gameId, String message){

=======
    
    public void sendToGame(String gameId, String message) {
>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b
        GameRoom room = rooms.get(gameId);

        if (room == null) {
            System.err.println(
                "Cannot broadcast: game room " + gameId + " not found."
            );
            return;
        }

        WebSocketSession player1 = room.getPlayer1();
        WebSocketSession player2 = room.getPlayer2();

        System.out.println(
            "Broadcasting to game " + gameId
                + " | Player 1 session: "
                + (player1 == null ? "null" : player1.getId())
                + " | Player 2 session: "
                + (player2 == null ? "null" : player2.getId())
        );

        sendToPlayer(player1, message);
        sendToPlayer(player2, message);
    }

<<<<<<< HEAD
    // send a message to a specific player
    // does nothing if the session is null or not open
    private void sendToPlayer(WebSocketSession session, String message) {
=======
    private void sendToPlayer(
            WebSocketSession session,
            String message) {
>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b

        if (session == null) {
            System.err.println("Broadcast skipped: session is null.");
            return;
        }

        if (!session.isOpen()) {
            System.err.println(
                "Broadcast skipped: session " + session.getId()
                    + " is closed."
            );
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));

            System.out.println(
                "Message sent to session " + session.getId()
                    + ": " + message
            );

        } catch (IOException e) {
            System.err.println(
                "Failed to send message to session "
                    + session.getId()
            );
            e.printStackTrace();
        }
    }

<<<<<<< HEAD
    // get the total number of active WebSocket connections
    // returns the number of connections as an integer
=======

>>>>>>> 3ab36ff44c66a7af0e16228bbfd08ce18faafa7b
    public int getConnectionCount() {
        return sessions.size();
    }

    // get the total number of game rooms
    // returns the number of game rooms as an integer
    public int getRoomCount() {
        return rooms.size();
    }
}