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
                            "start": 40,
                            "end": 33
                        }
                    },
                    {
                        "move_number": 2,
                        "move_data": {
                            "start": 17,
                            "end": 24
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

        // First move: red moves from square 40 to 33.
        GameLogic firstMove = replay.next();

        assertEquals(1, replay.getPosition());
        assertTrue(firstMove.getBoard().getPiece(40).isEmpty());
        assertTrue(firstMove.getBoard().getPiece(33).isRed());

        // Second move: black moves from square 17 to 24.
        GameLogic secondMove = replay.next();

        assertEquals(2, replay.getPosition());
        assertTrue(secondMove.getBoard().getPiece(17).isEmpty());
        assertTrue(secondMove.getBoard().getPiece(24).isBlack());

        // Going back restores black's original square.
        GameLogic previousMove = replay.previous();

        assertEquals(1, replay.getPosition());
        assertTrue(previousMove.getBoard().getPiece(17).isBlack());
        assertTrue(previousMove.getBoard().getPiece(24).isEmpty());

        // Restart restores the initial board.
        GameLogic initialGame = replay.restart();

        assertEquals(0, replay.getPosition());
        assertTrue(initialGame.getBoard().getPiece(40).isRed());
        assertTrue(initialGame.getBoard().getPiece(33).isEmpty());
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
        returnedGame.getBoard().removePiece(40);

        assertTrue(
                replay.getCurrentGame()
                    .getBoard().getPiece(40).isRed()
        );
    }
        @Test
    void replayCaptureAndRestoreCapturedPiece() throws Exception {

        ArrayNode moves = (ArrayNode) new ObjectMapper().readTree("""
                [
                    {
                        "move_number": 1,
                        "move_data": {
                            "start": 40,
                            "end": 33
                        }
                    },
                    {
                        "move_number": 2,
                        "move_data": {
                            "start": 19,
                            "end": 26
                        }
                    },
                    {
                        "move_number": 3,
                        "move_data": {
                            "start": 33,
                            "end": 19
                        }
                    }
                ]
                """);

        GameReplay replay = new GameReplay(moves);

        // Before capture: red is at 33, black is at 26.
        GameLogic beforeCapture = replay.seek(2);

        assertTrue(beforeCapture.getBoard().getPiece(33).isRed());
        assertTrue(beforeCapture.getBoard().getPiece(26).isBlack());

        // Red captures black and lands at 19.
        GameLogic afterCapture = replay.next();

        assertEquals(3, replay.getPosition());
        assertTrue(afterCapture.getBoard().getPiece(19).isRed());
        assertTrue(afterCapture.getBoard().getPiece(26).isEmpty());
        assertTrue(afterCapture.getBoard().getPiece(33).isEmpty());

        // Going backward restores the captured piece.
        GameLogic restored = replay.previous();

        assertEquals(2, replay.getPosition());
        assertTrue(restored.getBoard().getPiece(33).isRed());
        assertTrue(restored.getBoard().getPiece(26).isBlack());
        assertTrue(restored.getBoard().getPiece(19).isEmpty());
    }
}