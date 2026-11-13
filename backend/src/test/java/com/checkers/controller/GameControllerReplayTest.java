package com.checkers.controller;

import com.checkers.database.MoveDAO;
import com.checkers.database.GameDAO;
import com.checkers.logic.GameLogic;
import com.checkers.websocket.GameSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameControllerReplayTest {

    private GameController createController() {
        GameSessionManager sessionManager = mock(GameSessionManager.class);
        when(sessionManager.createGameLogic(anyString()))
                .thenReturn(new GameLogic());
        return new GameController(sessionManager);
    }

    private HttpSession loggedInSession(long userId) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("userId")).thenReturn(userId);
        return session;
    }

    private ArrayNode savedMoves() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GameLogic game = new GameLogic();
        int start = -1;
        int end = -1;
        for (int index = 0; index < game.getBoard().getCellCount(); index++) {
            if (!game.isCurrentPlayerPiece(index)) {
                continue;
            }
            var destinations = game.getLegalDestinations(index);
            if (!destinations.isEmpty()) {
                start = index;
                end = destinations.getFirst();
                break;
            }
        }
        ArrayNode moves = mapper.createArrayNode();
        var savedMove = moves.addObject();
        savedMove.put("move_number", 1);
        savedMove.putObject("move_data").put("start", start).put("end", end);
        return moves;
    }

    private JsonNode savedGame() throws Exception {
        return new ObjectMapper().readTree("""
                {
                  "id": 8,
                  "status": "COMPLETED",
                  "variant_id": "AMERICAN"
                }
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

        try (var gameDatabase = mockStatic(GameDAO.class);
             var database = mockStatic(MoveDAO.class)) {
            gameDatabase.when(() -> GameDAO.isParticipant(8, 13))
                    .thenReturn(true);
            gameDatabase.when(() -> GameDAO.getGame(8))
                    .thenReturn(savedGame());
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

        try (var gameDatabase = mockStatic(GameDAO.class);
             var database = mockStatic(MoveDAO.class)) {
            gameDatabase.when(() -> GameDAO.isParticipant(8, 99))
                    .thenReturn(false);
            database.when(() -> MoveDAO.getMovesForReplay(8))
                    .thenReturn(savedMoves());

            var response = controller.getReplay(8, null, session);

            assertEquals(403, response.getStatusCode().value());
        }
    }

    @Test
    void databaseErrorsDoNotExposeDetails() throws Exception {
        GameController controller = createController();
        HttpSession session = loggedInSession(13);

        try (var gameDatabase = mockStatic(GameDAO.class);
             var database = mockStatic(MoveDAO.class)) {
            gameDatabase.when(() -> GameDAO.isParticipant(8, 13))
                    .thenReturn(true);
            gameDatabase.when(() -> GameDAO.getGame(8))
                    .thenReturn(savedGame());
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
}