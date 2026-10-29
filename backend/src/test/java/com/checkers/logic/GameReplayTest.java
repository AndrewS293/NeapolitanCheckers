/*package com.checkers.logic;

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
}*/