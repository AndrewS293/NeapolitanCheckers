package com.checkers.websocket;

import com.checkers.logic.Board;
import com.checkers.logic.GameLogic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameWebSocketHandlerTest {
    private static final String GAME_ID = "GAME01";

    private GameSessionManager sessionManager;
    private GameWebSocketHandler handler;
    private WebSocketSession playerOne;
    private WebSocketSession playerTwo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        sessionManager = new GameSessionManager();
        handler = new GameWebSocketHandler(sessionManager);
        sessionManager.createRoom(GAME_ID, 1L, GameVisibility.PUBLIC, null);
        playerOne = createSession("session-one", "alice", GAME_ID);
        playerTwo = createSession("session-two", "bob", GAME_ID);
        handler.afterConnectionEstablished(playerOne);
        handler.afterConnectionEstablished(playerTwo);
    }

    @AfterEach
    void cleanUpReconnectTimeouts() {
        sessionManager.cancelReconnectTimeout(GAME_ID, 1);
        sessionManager.cancelReconnectTimeout(GAME_ID, 2);
    }

    //A.C. 6.1 Disconnect from game
    @Test
    void disconnectingPlayerRemovesTheirConnectionFromTheGame() throws Exception {
        when(playerOne.isOpen()).thenReturn(false);

        handler.afterConnectionClosed(playerOne, CloseStatus.NORMAL);

        GameRoom room = sessionManager.getRoom(GAME_ID);
        assertNotNull(room);
        assertNull(room.getPlayer1());
        assertEquals(1, room.getPlayerCount());
        assertEquals(GameRoom.Status.WAITING, room.getStatus());
        assertEquals(1, sessionManager.getConnectionCount());
    }

    //A.C. 6.2 Notify remaining player of disconnect
    @Test
    void remainingPlayerReceivesDisconnectNotification() throws Exception {
        clearInvocations(playerTwo);
        when(playerOne.isOpen()).thenReturn(false);

        handler.afterConnectionClosed(playerOne, CloseStatus.NORMAL);

        JsonNode notification = findMessage(playerTwo, "PLAYER_DISCONNECTED");
        assertEquals("alice", notification.get("username").asText());
        assertTrue(notification.get("message").asText().contains("disconnected"));
        assertNotNull(findMessage(playerTwo, "GAME_WAITING"));
    }

    //A.C. 6.3 Connect to new game
    @Test
    void disconnectedPlayerCanJoinAnotherGame() throws Exception {
        when(playerOne.isOpen()).thenReturn(false);
        handler.afterConnectionClosed(playerOne, CloseStatus.NORMAL);

        sessionManager.createRoom("GAME02", 2L, GameVisibility.PUBLIC, null);
        WebSocketSession replacementSession = createSession("session-one-new", "alice", "GAME02");
        handler.afterConnectionEstablished(replacementSession);
        WebSocketSession newOpponent = createSession("session-three", "carol", "GAME02");
        handler.afterConnectionEstablished(newOpponent);

        GameRoom newRoom = sessionManager.getRoom("GAME02");
        assertEquals(replacementSession, newRoom.getPlayer1());
        assertEquals("alice", newRoom.getPlayer1Username());
        assertEquals(newOpponent, newRoom.getPlayer2());
        assertEquals(2, newRoom.getPlayerCount());
        assertEquals(GameRoom.Status.ACTIVE, newRoom.getStatus());
    }

    //A.C. 10.1 Game starts when both players are connected
    @Test
    void twoConnectedPlayersActivateGameAndReceiveStartedEvent() throws Exception {
        GameRoom room = sessionManager.getRoom(GAME_ID);

        assertEquals(GameRoom.Status.ACTIVE, room.getStatus());
        assertTrue(room.isPlayerConnected(1));
        assertTrue(room.isPlayerConnected(2));
        assertEquals(GameLogic.red_player, room.getGameLogic().getCurrentPlayer());
        assertNotNull(findMessage(playerOne, "GAME_STARTED"));
        assertNotNull(findMessage(playerTwo, "GAME_STARTED"));
    }

    //11.2 Opponent Cannot Move on My First Turn
    @Test
    void opponentCannotMoveWhenPlayerOneHasTheFirstTurn() throws Exception {
        GameRoom room = sessionManager.getRoom(GAME_ID);
        prepareTurnTestBoard(room);
        clearInvocations(playerTwo);

        handler.handleTextMessage(playerTwo, moveMessage(2, 1, 3, 0));

        assertEquals(GameLogic.red_player, room.getGameLogic().getCurrentPlayer());
        assertEquals(PieceType.BLACK, pieceType(room, 2, 1));
        assertNotNull(findMessage(playerTwo, "ERROR"));
        assertFalse(hasMessage(playerTwo, "MOVE"));
    }

    //11.1 Designated First Player 11.3 Player Can Move on Their First Turn
    @Test
    void firstPlayerCanMoveOnTheirFirstTurn() throws Exception {
        GameRoom room = sessionManager.getRoom(GAME_ID);
        prepareTurnTestBoard(room);
        clearInvocations(playerOne);

        handler.handleTextMessage(playerOne, moveMessage(5, 0, 4, 1));

        assertEquals(GameLogic.black_player, room.getGameLogic().getCurrentPlayer());
        assertEquals(PieceType.RED, pieceType(room, 4, 1));
        assertNotNull(findMessage(playerOne, "MOVE"));
    }

    private WebSocketSession createSession(String id, String username, String gameId) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", username);

        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getUri()).thenReturn(new URI("ws://localhost/game?gameId=" + gameId));
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private TextMessage moveMessage(int fromRow, int fromCol, int toRow, int toCol) {
        return new TextMessage("""
                {"type":"MOVE","message":{"fromRow":%d,"fromCol":%d,"toRow":%d,"toCol":%d}}
                """.formatted(fromRow, fromCol, toRow, toCol));
    }

    private JsonNode findMessage(WebSocketSession session, String type) throws Exception {
        var messageCaptor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(messageCaptor.capture());

        for (TextMessage message : messageCaptor.getAllValues()) {
            JsonNode json = objectMapper.readTree(message.getPayload());
            if (type.equals(json.path("type").asText())) {
                return json;
            }
        }

        fail("Expected WebSocket message type " + type);
        return null;
    }

    private boolean hasMessage(WebSocketSession session, String type) throws Exception {
        var messageCaptor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(messageCaptor.capture());

        for (TextMessage message : messageCaptor.getAllValues()) {
            if (type.equals(objectMapper.readTree(message.getPayload()).path("type").asText())) {
                return true;
            }
        }
        return false;
    }

    private int pieceType(GameRoom room, int row, int column) {
        int square = Board.toIndex(row, column);
        return room.getGameLogic().getBoard().getPiece(square).getType();
    }

    private void prepareTurnTestBoard(GameRoom room) {
        Board board = room.getGameLogic().getBoard();
        board.clear();
        board.setPiece(Board.toIndex(5, 0), new com.checkers.logic.Piece(com.checkers.logic.Piece.red));
        board.setPiece(Board.toIndex(2, 1), new com.checkers.logic.Piece(com.checkers.logic.Piece.black));
    }

    private static final class PieceType {
        private static final int RED = com.checkers.logic.Piece.red;
        private static final int BLACK = com.checkers.logic.Piece.black;
    }

}