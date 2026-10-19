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

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

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


    // create a new game room
    // stores the room in the rooms map using its game ID as the key
    public GameRoom createRoom(String gameId, GameVisibility visibility, String joinCode) {
        GameRoom room = new GameRoom(gameId, visibility, joinCode);
        rooms.put(gameId, room);

        System.out.println(
                "Created " + visibility.name().toLowerCase() + " game room: " + gameId
        );

        return room;
    }

    // get a game room by its game ID
    // returns the game room if found, null otherwise
    public GameRoom getRoom(String gameId) {
        return rooms.get(gameId);
    }


    // get a game room by its join code
    // returns the game room if found, null otherwise
    public GameRoom getRoomByJoinCode(String joinCode) {
        for (GameRoom room : rooms.values()) {
            if (room.getJoinCode().equals(joinCode)) {
                return room;
            }
        }
        return null;
    }

    // get all open public games
    // returns a list of game rooms that are public, waiting, and not full
    public List<GameRoom> getOpenPublicGames() {
        List<GameRoom> openPublicGames = new ArrayList<>();

        for (GameRoom room : rooms.values()) {
            if (room.getVisibility() == GameVisibility.PUBLIC && room.getStatus() == GameRoom.Status.WAITING && !room.isFull()) {
                openPublicGames.add(room);
            }
        }

        return openPublicGames;
    }

    // allow a player to join a game room
    // returns true if the player successfully joined, false otherwise
    public boolean joinRoom(String gameId, WebSocketSession session, String username) {
        GameRoom room = rooms.get(gameId);
        
        if (room == null)  {
            return false;
        }

        if (room.isFull()) {
            return false;
        }

        boolean added = room.addPlayer(session, username);

        if (added) {
            System.out.println(
                    "Client " + session.getId()
                            + " joined room: " + gameId
            );
        }

        System.out.println("Players in game: " +room.getPlayerCount());
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
                "Client " + session.getId()
                        + " left game " + gameId
        );

        System.out.println(
                "Players in game: "
                        + room.getPlayerCount()
        );

        if (room.isEmpty()) {

            rooms.remove(gameId);

            System.out.println(
                    "Removed empty game room: " + gameId
            );
        }
    }

    // send a message to all players in a game room
    // does nothing if the game room does not exist
    public void sendToGame(String gameId, String message){

        GameRoom room = rooms.get(gameId);

        if (room == null) {
            return;
        }

        sendToPlayer(room.getPlayer1(), message);
        sendToPlayer(room.getPlayer2(), message);
    }

    // send a message to a specific player
    // does nothing if the session is null or not open
    private void sendToPlayer(WebSocketSession session, String message) {

        if(session == null || !session.isOpen()) {
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            System.err.println(
                    "Failed to send message to "
                            + session.getId()
            );
        }
    }

    // get the total number of active WebSocket connections
    // returns the number of connections as an integer
    public int getConnectionCount() {
        return sessions.size();
    }

    // get the total number of game rooms
    // returns the number of game rooms as an integer
    public int getRoomCount() {
        return rooms.size();
    }
}