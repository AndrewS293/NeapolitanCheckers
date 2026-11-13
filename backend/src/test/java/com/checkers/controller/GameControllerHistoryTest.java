package com.checkers.controller;

import com.checkers.database.GameDAO;
import com.checkers.websocket.GameSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GameControllerHistoryTest {
    @Test
    void playerCanRetrieveTheirCompletedGameHistory() throws Exception {
        GameController controller = new GameController(
                mock(GameSessionManager.class));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("userId")).thenReturn(17L);
        var games = new ObjectMapper().readTree("""
                [{
                  "gameId": 91,
                  "startedAt": "2026-11-13T10:00:00Z",
                  "opponent": "AI",
                  "variant": "AMERICAN",
                  "outcome": "LOSS"
                }]
                """);

        try (var database = mockStatic(GameDAO.class)) {
            database.when(() -> GameDAO.getCompletedGamesForPlayer(17L))
                    .thenReturn((com.fasterxml.jackson.databind.node.ArrayNode) games);

            var response = controller.getCompletedGamesForPlayer(17L, session);

            assertEquals(200, response.getStatusCode().value());
            assertEquals(games, response.getBody());
        }
    }

    @Test
    void playerCannotRetrieveAnotherPlayersHistory() {
        GameController controller = new GameController(
                mock(GameSessionManager.class));
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("userId")).thenReturn(17L);

        try (var database = mockStatic(GameDAO.class)) {
            var response = controller.getCompletedGamesForPlayer(18L, session);

            assertEquals(403, response.getStatusCode().value());
            database.verifyNoInteractions();
        }
    }

    @Test
    void historyRequiresLogin() {
        GameController controller = new GameController(
                mock(GameSessionManager.class));
        HttpSession session = mock(HttpSession.class);

        try (var database = mockStatic(GameDAO.class)) {
            var response = controller.getCompletedGamesForPlayer(17L, session);

            assertEquals(401, response.getStatusCode().value());
            database.verifyNoInteractions();
        }
    }
}
