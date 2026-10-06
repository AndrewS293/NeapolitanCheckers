package com.checkers.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

public class GameRoomTest {


    private GameRoom gameSession; // Replace with your actual class name
    private WebSocketSession mockSession1;
    private WebSocketSession mockSession2;
    private WebSocketSession mockSession3;

    @BeforeEach
    void setUp() {
        gameSession = new GameRoom("ABC123", 123456, GameVisibility.PUBLIC, "ABCDEF");
        mockSession1 = mock(WebSocketSession.class);
        mockSession2 = mock(WebSocketSession.class);
        mockSession3 = mock(WebSocketSession.class);
    }

    @Test
    void testAddFirstPlayer_Success() {
        boolean result = gameSession.addPlayer(mockSession1, "Player1");

        assertTrue(result, "Adding the first player should return true");
        assertEquals(mockSession1, gameSession.getPlayer1(), "player1 session should match");
        assertEquals("Player1", gameSession.getPlayer1Username(), "player1 username should match");
    }

    @Test
    void testAddSecondPlayer_Success() {
        gameSession.addPlayer(mockSession1, "Player1");

        
        boolean result = gameSession.addPlayer(mockSession2, "Player2");

        assertTrue(result, "Adding the second player should return true");
        assertEquals(mockSession2, gameSession.getPlayer2(), "player2 session should match");
        assertEquals("Player2", gameSession.getPlayer2Username(), "player2 username should match");
    }

    @Test
    void testAddThirdPlayer_Fails() {
        gameSession.addPlayer(mockSession1, "Player1");
        gameSession.addPlayer(mockSession2, "Player2");

        
        boolean result = gameSession.addPlayer(mockSession3, "Player3");

        assertFalse(result, "Adding a third player to a full game should return false");
    }





    @Test
    void testGetDatabaseGameId() {

    }

    @Test
    void testGetGameId() {

    }

    @Test
    void testGetJoinCode() {

    }

    @Test
    void testGetPlayer1() {

    }

    @Test
    void testGetPlayer1Username() {

    }

    @Test
    void testGetPlayer2() {

    }

    @Test
    void testGetPlayer2Username() {

    }

    @Test
    void testGetPlayerCount() {

    }

    @Test
    void testGetStatus() {

    }

    @Test
    void testGetVisibility() {

    }

    @Test
    void testIsEmpty() {

    }

    @Test
    void testIsFull() {

    }

    @Test
    void testRemovePlayer() {

    }
}
