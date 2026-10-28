package com.checkers.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameReplayTest {

    private ArrayNode exampleMoves() throws Exception {
        return (ArrayNode) new ObjectMapper().readTree("""
                [
                    {
                        "move_number": 1,
                        "move_data": {
                            "start": 20,
                            "end": 16
                        }
                    },
                    {
                        "move_number": 2,
                        "move_data": {
                            "start": 8,
                            "end": 12
                        }
                    }
                ]
                """);
    }

    @Test
    void nextPreviousAndRestartRestoreTheBoard() throws Exception {
        GameReplay replay = new GameReplay(exampleMoves());

        assertEquals(0, replay.getPosition());
        assertEquals(2, replay.getMoveCount());

        // First move: red moves from square 20 to 16.
        GameLogic firstMove = replay.next();

        assertEquals(1, replay.getPosition());
        assertTrue(firstMove.getBoard().getPiece(20).isEmpty());
        assertTrue(firstMove.getBoard().getPiece(16).isRed());

        // Second move: black moves from square 8 to 12.
        GameLogic secondMove = replay.next();

        assertEquals(2, replay.getPosition());
        assertTrue(secondMove.getBoard().getPiece(8).isEmpty());
        assertTrue(secondMove.getBoard().getPiece(12).isBlack());

        // Going back restores black's original square.
        GameLogic previousMove = replay.previous();

        assertEquals(1, replay.getPosition());
        assertTrue(previousMove.getBoard().getPiece(8).isBlack());
        assertTrue(previousMove.getBoard().getPiece(12).isEmpty());

        // Restart restores the initial board.
        GameLogic initialGame = replay.restart();

        assertEquals(0, replay.getPosition());
        assertTrue(initialGame.getBoard().getPiece(20).isRed());
        assertTrue(initialGame.getBoard().getPiece(16).isEmpty());
    }

    @Test
    void playPauseAndSeekControlPlayback() throws Exception {
        GameReplay replay = new GameReplay(exampleMoves());

        replay.play();
        replay.tick();

        assertEquals(1, replay.getPosition());

        replay.pause();
        replay.tick();

        assertEquals(1, replay.getPosition());

        replay.play();
        replay.tick();

        assertEquals(2, replay.getPosition());
        assertFalse(replay.isPlaying());

        replay.seek(0);

        assertEquals(0, replay.getPosition());
        assertThrows(
                IllegalArgumentException.class,
                () -> replay.seek(3)
        );
    }

    @Test
    void changingReturnedBoardDoesNotChangeReplay() throws Exception {
        GameReplay replay = new GameReplay(exampleMoves());

        GameLogic returnedGame = replay.getCurrentGame();
        returnedGame.getBoard().removePiece(20);

        assertTrue(
                replay.getCurrentGame()
                        .getBoard().getPiece(20).isRed()
        );
    }
        @Test
    void replayCaptureAndRestoreCapturedPiece() throws Exception {

        ArrayNode moves = (ArrayNode) new ObjectMapper().readTree("""
                [
                    {
                        "move_number": 1,
                        "move_data": {
                            "start": 20,
                            "end": 16
                        }
                    },
                    {
                        "move_number": 2,
                        "move_data": {
                            "start": 9,
                            "end": 13
                        }
                    },
                    {
                        "move_number": 3,
                        "move_data": {
                            "start": 16,
                            "end": 9
                        }
                    }
                ]
                """);

        GameReplay replay = new GameReplay(moves);

        // Before capture: red is at 16, black is at 13.
        GameLogic beforeCapture = replay.seek(2);

        assertTrue(beforeCapture.getBoard().getPiece(16).isRed());
        assertTrue(beforeCapture.getBoard().getPiece(13).isBlack());

        // Red captures black and lands at 9.
        GameLogic afterCapture = replay.next();

        assertEquals(3, replay.getPosition());
        assertTrue(afterCapture.getBoard().getPiece(9).isRed());
        assertTrue(afterCapture.getBoard().getPiece(13).isEmpty());
        assertTrue(afterCapture.getBoard().getPiece(16).isEmpty());

        // Going backward restores the captured piece.
        GameLogic restored = replay.previous();

        assertEquals(2, replay.getPosition());
        assertTrue(restored.getBoard().getPiece(16).isRed());
        assertTrue(restored.getBoard().getPiece(13).isBlack());
        assertTrue(restored.getBoard().getPiece(9).isEmpty());
    }
}