package com.checkers.controller;

import com.checkers.database.GameDAO;
import com.checkers.database.MoveDAO;
import com.checkers.websocket.GameSessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameControllerLocalPersistenceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completedLocalGameSavesMovesAndReturnsItsDatabaseId() throws Exception {
        GameSessionManager sessionManager = mock(GameSessionManager.class);
        when(sessionManager.isSupportedVariant("AMERICAN")).thenReturn(true);
        GameController controller = new GameController(sessionManager);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("userId")).thenReturn(17L);
        Map<String, Object> request = objectMapper.readValue("""
                {
                  "variantId": "AMERICAN",
                  "opponent": "AI",
                  "result": "RED_WON",
                  "winnerPlayer": 1,
                  "moves": [
                    {
                      "start": 40,
                      "end": 33,
                      "fromRow": 5,
                      "fromCol": 0,
                      "toRow": 4,
                      "toCol": 1,
                      "gameState": "RED_WON"
                    }
                  ]
                }
                """, new TypeReference<>() {});

        try (var gameDatabase = mockStatic(GameDAO.class);
             var moveDatabase = mockStatic(MoveDAO.class)) {
            gameDatabase.when(() -> GameDAO.createGame(
                    anyString(), eq("AMERICAN"))).thenReturn(91L);
            gameDatabase.when(() -> GameDAO.registerPlayer(91L, 17L, 1))
                    .thenReturn(true);
            moveDatabase.when(() -> MoveDAO.saveMove(
                    eq(91L), eq(17L), eq(1), anyMap())).thenReturn(true);
            gameDatabase.when(() -> GameDAO.completeGame(91L, 17L))
                    .thenReturn(true);

            var response = controller.completeLocalGame(request, session);

            assertEquals(200, response.getStatusCode().value());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertEquals(91L, body.get("databaseGameId"));
            assertEquals("COMPLETED", body.get("status"));
            gameDatabase.verify(() -> GameDAO.completeGame(91L, 17L));
            moveDatabase.verify(() -> MoveDAO.saveMove(
                    eq(91L), eq(17L), eq(1), anyMap()));
        }
    }

    @Test
    void completedLocalGameRequiresLogin() throws Exception {
        GameController controller = new GameController(
                mock(GameSessionManager.class));
        HttpSession session = mock(HttpSession.class);

        try (var gameDatabase = mockStatic(GameDAO.class);
             var moveDatabase = mockStatic(MoveDAO.class)) {
            var response = controller.completeLocalGame(
                    Map.of(), session);

            assertEquals(401, response.getStatusCode().value());
            gameDatabase.verifyNoInteractions();
            moveDatabase.verifyNoInteractions();
        }
    }
}
