package com.checkers.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameSessionManagerTest {

    private GameSessionManager manager;
    private WebSocketSession p1;
    private WebSocketSession p2;

    @BeforeEach
    void setUp() {
        manager = new GameSessionManager();
        p1 = mock(WebSocketSession.class);
        p2 = mock(WebSocketSession.class);
        when(p1.getId()).thenReturn("p1");
        when(p2.getId()).thenReturn("p2");
    }

    @Test
    void createRoom_StoresRoomAndReturnsIt() {
        GameRoom room = manager.createRoom("ABC123", 10L, GameVisibility.PUBLIC, null);

        assertNotNull(room);
        assertSame(room, manager.getRoom("ABC123"));
        assertEquals(1, manager.getRoomCount());
    }

    @Test
    void getRoomByJoinCode_ReturnsMatchingPrivateGame() {
        manager.createRoom("GAME1", 1L, GameVisibility.PRIVATE, "JOIN99");

        GameRoom room = manager.getRoomByJoinCode("JOIN99");

        assertNotNull(room);
        assertEquals("GAME1", room.getGameId());
    }

    @Test
    void getOpenPublicGames_ReturnsOnlyOpenPublicWaitingRooms() {
        GameRoom publicWaiting = manager.createRoom("A", 1L, GameVisibility.PUBLIC, null);
        GameRoom publicFull = manager.createRoom("B", 2L, GameVisibility.PUBLIC, null);
        GameRoom privateWaiting = manager.createRoom("C", 3L, GameVisibility.PRIVATE, "X");

        publicWaiting.addPlayer(p1, "alice");
        publicFull.addPlayer(p1, "bob");
        publicFull.addPlayer(p2, "carol");
        privateWaiting.addPlayer(p1, "dave");

        List<GameRoom> openGames = manager.getOpenPublicGames();

        assertEquals(1, openGames.size());
        assertTrue(openGames.contains(publicWaiting));
        assertFalse(openGames.contains(publicFull));
        assertFalse(openGames.contains(privateWaiting));
    }

    @Test
    void joinRoom_Success_WhenRoomHasSpace() {
        manager.createRoom("GAME1", 1L, GameVisibility.PUBLIC, null);

        boolean joined = manager.joinRoom("GAME1", p1, "alice");

        assertTrue(joined);
        assertEquals(1, manager.getRoom("GAME1").getPlayerCount());
    }

    @Test
    void joinRoom_ReturnsFalse_WhenRoomIsFull() {
        GameRoom room = manager.createRoom("GAME1", 1L, GameVisibility.PUBLIC, null);
        room.addPlayer(p1, "alice");
        room.addPlayer(p2, "bob");

        boolean joined = manager.joinRoom("GAME1", mock(WebSocketSession.class), "charlie");

        assertFalse(joined);
        assertEquals(2, room.getPlayerCount());
    }

    @Test
    void removePlayerFromGame_RemovesPlayerAndDeletesEmptyRoom() {
        manager.createRoom("GAME1", 1L, GameVisibility.PUBLIC, null);
        manager.joinRoom("GAME1", p1, "alice");

        manager.removePlayerFromGame("GAME1", p1);

        assertNull(manager.getRoom("GAME1"));
    }

    @Test
    void removePlayerFromGame_LeavesRoom_WhenOtherPlayerStillPresent() {
        manager.createRoom("GAME1", 1L, GameVisibility.PUBLIC, null);
        manager.joinRoom("GAME1", p1, "alice");
        manager.joinRoom("GAME1", p2, "bob");

        manager.removePlayerFromGame("GAME1", p1);

        assertNotNull(manager.getRoom("GAME1"));
        assertEquals(1, manager.getRoom("GAME1").getPlayerCount());
        assertEquals("bob", manager.getRoom("GAME1").getPlayer2Username());
    }
}
