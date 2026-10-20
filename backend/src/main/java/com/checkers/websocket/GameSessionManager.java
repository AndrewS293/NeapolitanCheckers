package com.checkers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class GameSessionManager {
    private static final long RECONNECT_TIMEOUT_SECONDS = 60;

    private final ScheduledExecutorService timeoutExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "checkers-reconnect-timeouts");
                thread.setDaemon(true);
                return thread;
            });

    private final Map<String, ScheduledFuture<?>> reconnectTimeouts =
            new ConcurrentHashMap<>();

    private String timeoutKey(String gameId, int playerNumber) {
        return gameId + ":" + playerNumber;
    }

    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private final Map<String, GameRoom> rooms =
            new ConcurrentHashMap<>();

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

    public GameRoom getRoom(String gameId) {
        return rooms.get(gameId);
    }

    public GameRoom getRoomByJoinCode(String joinCode) {

        for (GameRoom room : rooms.values()) {

            if (joinCode != null &&
                    joinCode.equals(room.getJoinCode())) {

                return room;
            }
        }

        return null;
    }

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

    public boolean joinRoom(
        String gameId,
        WebSocketSession session,
        String username) {

        GameRoom room = rooms.get(gameId);

        if (room == null) {
            return false;
        }

        boolean added = room.addPlayer(session, username);

        if (added) {
            System.out.println(
                    "Client " + session.getId()
                            + " joined or reconnected to room: " + gameId
            );
        }

        System.out.println(
                "Connected players in game: " + room.getPlayerCount()
        );

        return added;
    }

    public void removePlayerFromGame(
        String gameId,
        WebSocketSession session) {

        GameRoom room = rooms.get(gameId);

        if (room == null) {
            return;
        }

        int playerNumber = room.disconnectPlayer(session);

        if (playerNumber == 0) {
            return;
        }

        System.out.println(
                "Player " + playerNumber
                        + " disconnected from game " + gameId
                        + ". Their slot is reserved for "
                        + RECONNECT_TIMEOUT_SECONDS + " seconds."
        );

        String key = timeoutKey(gameId, playerNumber);

        ScheduledFuture<?> timeout = timeoutExecutor.schedule(() -> {
            GameRoom currentRoom = rooms.get(gameId);

            if (currentRoom == null) {
                return;
            }

            synchronized (currentRoom) {
                // A successful reconnect or a finished game cancels the timeout.
                if (currentRoom.getStatus() == GameRoom.Status.FINISHED
                        || currentRoom.isPlayerConnected(playerNumber)) {
                    reconnectTimeouts.remove(key);
                    return;
                }

                currentRoom.finish();
            }

            reconnectTimeouts.remove(key);

            String message = "{\"type\":\"RECONNECT_TIMEOUT\","
                    + "\"gameId\":\"" + gameId + "\","
                    + "\"player\":" + playerNumber + ","
                    + "\"status\":\"FINISHED\","
                    + "\"message\":\"The disconnected player's reconnection "
                    + "time expired. The match has ended.\"}";

            sendToGame(gameId, message);

            System.out.println(
                    "Reconnect timeout expired for player "
                            + playerNumber + " in game " + gameId
            );

        }, RECONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        ScheduledFuture<?> previous = reconnectTimeouts.put(key, timeout);

        if (previous != null) {
            previous.cancel(false);
        }
    }

    public void cancelReconnectTimeout(
        String gameId,
        int playerNumber) {

        String key = timeoutKey(gameId, playerNumber);

        ScheduledFuture<?> timeout = reconnectTimeouts.remove(key);

        if (timeout != null) {
            timeout.cancel(false);
        }
    }

    
    public void sendToGame(String gameId, String message) {
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

    private void sendToPlayer(
            WebSocketSession session,
            String message) {

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


    public int getConnectionCount() {
        return sessions.size();
    }

    public int getRoomCount() {
        return rooms.size();
    }
}