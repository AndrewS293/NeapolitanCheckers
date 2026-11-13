package com.checkers.controller;

import com.checkers.websocket.GameRoom;
import com.checkers.websocket.GameSessionManager;
import com.checkers.websocket.GameVisibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GameControllerJoinTest {
    @Test
    void publicJoinReturnsRoomAndDatabaseIds() {
        GameSessionManager sessionManager = mock(GameSessionManager.class);
        GameRoom room = mock(GameRoom.class);
        when(sessionManager.getRoom("E3753A")).thenReturn(room);
        when(room.getVisibility()).thenReturn(GameVisibility.PUBLIC);
        when(room.isFull()).thenReturn(false);
        when(room.getGameId()).thenReturn("E3753A");
        when(room.getDatabaseGameId()).thenReturn(137L);
        when(room.getStatus()).thenReturn(GameRoom.Status.WAITING);
        when(room.getPlayerCount()).thenReturn(1);
        when(room.getVariantId()).thenReturn("AMERICAN");

        var response = new GameController(sessionManager)
                .joinPublicGame("E3753A");

        assertEquals(200, response.getStatusCode().value());
        var body = (java.util.Map<?, ?>) response.getBody();
        assertEquals("E3753A", body.get("gameId"));
        assertEquals(137L, body.get("databaseGameId"));
    }

    @Test
    void publicJoinReportsUnknownRoomWithoutThrowing() {
        GameSessionManager sessionManager = mock(GameSessionManager.class);
        when(sessionManager.getRoom("E3753A")).thenReturn(null);

        var response = new GameController(sessionManager)
                .joinPublicGame("E3753A");

        assertEquals(404, response.getStatusCode().value());
    }
}
