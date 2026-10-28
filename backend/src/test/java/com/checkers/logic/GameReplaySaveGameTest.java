package com.checkers.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class GameReplaySavedGameTest {

    private GameReplay loadSavedGame() throws Exception {

        try (InputStream input = getClass()
                .getResourceAsStream("/game-48-moves.json")) {

            assertNotNull(
                    input,
                    "Put game-48-moves.json in src/test/resources."
            );

            JsonNode moves = new ObjectMapper().readTree(input);

            assertTrue(moves.isArray());
            assertEquals(30, moves.size());

            return new GameReplay((ArrayNode) moves);
        }
    }

    @Test
    void replayPromotionAndUndoIt() throws Exception {

        GameReplay replay = loadSavedGame();

        // Move 21 leaves red at d6 with another jump required.
        GameLogic beforePromotion = replay.seek(21);

        assertTrue(beforePromotion.getBoard().getPiece(9).isRed());
        assertFalse(beforePromotion.getBoard().getPiece(9).isKing());
        assertTrue(beforePromotion.getBoard().getPiece(6).isBlack());

        assertEquals(GameLogic.red_player,
                beforePromotion.getCurrentPlayer());
        assertEquals(9, beforePromotion.getForcedJumpIndex());

        // Move 22: d6 to f8, capturing e7 and becoming a king.
        GameLogic promoted = replay.next();

        assertEquals(22, replay.getPosition());
        assertTrue(promoted.getBoard().getPiece(2).isRed());
        assertTrue(promoted.getBoard().getPiece(2).isKing());
        assertTrue(promoted.getBoard().getPiece(9).isEmpty());
        assertTrue(promoted.getBoard().getPiece(6).isEmpty());

        assertEquals(GameLogic.black_player,
                promoted.getCurrentPlayer());
        assertEquals(-1, promoted.getForcedJumpIndex());

        // Going backward restores the regular red piece and black piece.
        GameLogic restored = replay.previous();

        assertEquals(21, replay.getPosition());
        assertTrue(restored.getBoard().getPiece(9).isRed());
        assertFalse(restored.getBoard().getPiece(9).isKing());
        assertTrue(restored.getBoard().getPiece(6).isBlack());
        assertTrue(restored.getBoard().getPiece(2).isEmpty());

        assertEquals(GameLogic.red_player,
                restored.getCurrentPlayer());
        assertEquals(9, restored.getForcedJumpIndex());
    }

    @Test
    void replayMultipleJumpAndUndoBothJumps() throws Exception {

        GameReplay replay = loadSavedGame();

        // Before move 29: black is at d6.
        GameLogic beforeJumps = replay.seek(28);

        assertTrue(beforeJumps.getBoard().getPiece(9).isBlack());
        assertTrue(beforeJumps.getBoard().getPiece(14).isRed());
        assertTrue(beforeJumps.getBoard().getPiece(23).isRed());

        // Move 29: d6 to f4, capturing e5.
        GameLogic firstJump = replay.next();

        assertEquals(29, replay.getPosition());
        assertTrue(firstJump.getBoard().getPiece(18).isBlack());
        assertTrue(firstJump.getBoard().getPiece(14).isEmpty());

        // Black must continue with the same piece.
        assertEquals(GameLogic.black_player,
                firstJump.getCurrentPlayer());
        assertEquals(18, firstJump.getForcedJumpIndex());

        // Move 30: f4 to h2, capturing g3.
        GameLogic secondJump = replay.next();

        assertEquals(30, replay.getPosition());
        assertTrue(secondJump.getBoard().getPiece(27).isBlack());
        assertTrue(secondJump.getBoard().getPiece(18).isEmpty());
        assertTrue(secondJump.getBoard().getPiece(23).isEmpty());

        assertEquals(GameLogic.red_player,
                secondJump.getCurrentPlayer());
        assertEquals(-1, secondJump.getForcedJumpIndex());

        // Undo the second jump.
        GameLogic undoSecond = replay.previous();

        assertTrue(undoSecond.getBoard().getPiece(18).isBlack());
        assertTrue(undoSecond.getBoard().getPiece(23).isRed());
        assertTrue(undoSecond.getBoard().getPiece(27).isEmpty());

        assertEquals(GameLogic.black_player,
                undoSecond.getCurrentPlayer());
        assertEquals(18, undoSecond.getForcedJumpIndex());

        // Undo the first jump.
        GameLogic undoFirst = replay.previous();

        assertEquals(28, replay.getPosition());
        assertTrue(undoFirst.getBoard().getPiece(9).isBlack());
        assertTrue(undoFirst.getBoard().getPiece(14).isRed());
        assertTrue(undoFirst.getBoard().getPiece(23).isRed());
        assertTrue(undoFirst.getBoard().getPiece(18).isEmpty());

        assertEquals(GameLogic.black_player,
                undoFirst.getCurrentPlayer());
        assertEquals(-1, undoFirst.getForcedJumpIndex());
    }
}