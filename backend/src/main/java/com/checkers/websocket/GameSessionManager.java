package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);

        System.out.println(
                "Client connected: " + session.getId()
        );

        System.out.println(
                "Active connections: " + sessions.size()
        );
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());

        System.out.println(
                "Client disconnected: " + session.getId()
        );

        System.out.println(
                "Active connections: " + sessions.size()
        );
    }


    public GameRoom createRoom(String gameId, GameVisibility visibility, String joinCode) {
        GameRoom room = new GameRoom(gameId, visibility, joinCode);
        rooms.put(gameId, room);

        System.out.println(
                "Created " + visibility.name().toLowerCase() + " game room: " + gameId
        );

        return room;
    }

    public GameRoom getRoom(String gameId) {
        return rooms.get(gameId);
    }


    public GameRoom getRoomByJoinCode(String joinCode) {
        for (GameRoom room : rooms.values()) {
            if (room.getJoinCode().equals(joinCode)) {
                return room;
            }
        }
        return null;
    }

    public List<GameRoom> getOpenPublicGames() {
        List<GameRoom> openPublicGames = new ArrayList<>();

        for (GameRoom room : rooms.values()) {
            if (room.getVisibility() == GameVisibility.PUBLIC && room.getStatus() == GameRoom.Status.WAITING && !room.isFull()) {
                openPublicGames.add(room);
            }
        }

        return openPublicGames;
    }

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

    public void sendToGame(String gameId, String message){

        GameRoom room = rooms.get(gameId);

        if (room == null) {
            return;
        }

        sendToPlayer(room.getPlayer1(), message);
        sendToPlayer(room.getPlayer2(), message);
    }

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

    public int getConnectionCount() {
        return sessions.size();
    }

    public int getRoomCount() {
        return rooms.size();
    }
}