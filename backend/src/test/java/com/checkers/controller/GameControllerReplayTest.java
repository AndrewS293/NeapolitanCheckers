/*package com.checkers.controller;

import com.checkers.database.MoveDAO;
import com.checkers.websocket.GameSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameControllerReplayTest {

    private GameController createController() {
        return new GameController(mock(GameSessionManager.class));
    }

    private HttpSession loggedInSession(long userId) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("userId")).thenReturn(userId);
        return session;
    }

    private ArrayNode savedMoves() throws Exception {
        return (ArrayNode) new ObjectMapper().readTree("""
                [
                    {
                        "game_id": 8,
                        "player_id": 13,
                        "move_number": 1,
                        "move_data": {
                            "start": 20,
                            "end": 16
                        }
                    }
                ]
                """);
    }

    @Test
    void loginIsRequired() {
        GameController controller = createController();
        HttpSession session = mock(HttpSession.class);

        try (var database = mockStatic(MoveDAO.class)) {
            var response = controller.getReplay(8, null, session);

            assertEquals(401, response.getStatusCode().value());
            database.verifyNoInteractions();
        }
    }

    @Test
    void participantCanReadMovesAndReplayPositions() throws Exception {
        GameController controller = createController();
        HttpSession session = loggedInSession(13);

        try (var database = mockStatic(MoveDAO.class)) {
            database.when(() -> MoveDAO.getMovesForReplay(8))
                    .thenReturn(savedMoves());

            assertEquals(
                    200,
                    controller.getReplay(8, null, session)
                            .getStatusCode().value()
            );

            assertEquals(
                    200,
                    controller.getReplay(8, 0, session)
                            .getStatusCode().value()
            );

            assertEquals(
                    200,
                    controller.getReplay(8, 1, session)
                            .getStatusCode().value()
            );

            assertEquals(
                    400,
                    controller.getReplay(8, 2, session)
                            .getStatusCode().value()
            );
        }
    }

    @Test
    void anotherUserCannotReadTheReplay() throws Exception {
        GameController controller = createController();
        HttpSession session = loggedInSession(99);

        try (var database = mockStatic(MoveDAO.class)) {
            database.when(() -> MoveDAO.getMovesForReplay(8))
                    .thenReturn(savedMoves());

            var response = controller.getReplay(8, null, session);

            assertEquals(403, response.getStatusCode().value());
        }
    }

    @Test
    void databaseErrorsDoNotExposeDetails() {
        GameController controller = createController();
        HttpSession session = loggedInSession(13);

        try (var database = mockStatic(MoveDAO.class)) {
            database.when(() -> MoveDAO.getMovesForReplay(8))
                    .thenThrow(new IllegalStateException(
                            "Private database details"
                    ));

            var response = controller.getReplay(8, null, session);

            assertEquals(503, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertFalse(
                    response.getBody().toString()
                            .contains("Private database details")
            );
        }
    }
}*/