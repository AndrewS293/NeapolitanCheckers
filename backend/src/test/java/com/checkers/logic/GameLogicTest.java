package com.checkers.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameLogicTest {

    @Test
    void forcesJumpWhenAnyCurrentPlayerPieceCanCapture() {
        GameLogic game = emptyGame();
        Board board = game.getBoard();

        int jumpingRed = square(5, 0);
        int otherRed = square(5, 4);

        board.setPiece(jumpingRed, new Piece(Piece.red));
        board.setPiece(otherRed, new Piece(Piece.red));
        board.setPiece(square(4, 1), new Piece(Piece.black));

        // A regular move cannot happen when another piece can jump.
        assertTrue(game.hasAnyJump(GameLogic.red_player));
        assertFalse(
                game.move(otherRed, square(4, 3)),
                "An ordinary move must be rejected while a jump exists");

        assertTrue(game.move(jumpingRed, square(3, 2)));
    }

    @Test
    void keepsSamePlayersTurnUntilMultiJumpIsComplete() {
        GameLogic game = emptyGame();
        Board board = game.getBoard();

        int start = square(5, 0);
        int firstLanding = square(3, 2);
        int secondLanding = square(1, 4);

        board.setPiece(start, new Piece(Piece.red));
        board.setPiece(square(4, 1), new Piece(Piece.black));
        board.setPiece(square(2, 3), new Piece(Piece.black));

        // Red must remain the current player after the first jump.
        assertTrue(game.move(start, firstLanding));
        assertEquals(GameLogic.red_player, game.getCurrentPlayer());
        assertEquals(firstLanding, game.getForcedJumpIndex());
        assertTrue(game.isMultiJumpInProgress());

        // The turn changes after the second jump finishes the sequence.
        assertTrue(game.move(firstLanding, secondLanding));
        assertEquals(GameLogic.black_player, game.getCurrentPlayer());
        assertFalse(game.isMultiJumpInProgress());
        assertEquals(0, game.countPieces(GameLogic.black_player));
        assertEquals(GameLogic.GameState.RED_WON, game.getGameState());
    }

    @Test
    void legalDestinationListContainsOnlyAllowedMoves() {
        GameLogic game = emptyGame();
        Board board = game.getBoard();

        int red = square(5, 2);

        board.setPiece(red, new Piece(Piece.red));
        board.setPiece(square(0, 1), new Piece(Piece.black));

        assertEquals(2, game.getLegalDestinations(red).size());
        assertTrue(game.getLegalDestinations(red).contains(square(4, 1)));
        assertTrue(game.getLegalDestinations(red).contains(square(4, 3)));
    }

    @Test
    void winsWhenOpponentHasNoPieces() {
        GameLogic game = emptyGame();
        Board board = game.getBoard();

        board.setPiece(square(2, 1), new Piece(Piece.red));
        board.setPiece(square(1, 2), new Piece(Piece.black));

        // Red captures Black's final piece and wins.
        assertTrue(game.move(square(2, 1), square(0, 3)));
        assertEquals(GameLogic.GameState.RED_WON, game.getGameState());
        assertEquals("Red wins!", game.getGameResultMessage());
    }

    @Test
    void winsWhenOpponentStillHasPieceButCannotMove() {
        GameLogic game = emptyGame();
        Board board = game.getBoard();

        board.setPiece(square(5, 0), new Piece(Piece.red));
        board.setPiece(square(7, 0), new Piece(Piece.black));

        // Black still has a piece, but it is trapped and cannot move.
        assertTrue(game.move(square(5, 0), square(4, 1)));
        assertEquals(1, game.countPieces(GameLogic.black_player));
        assertFalse(game.hasAnyLegalMove(GameLogic.black_player));
        assertEquals(GameLogic.GameState.RED_WON, game.getGameState());
    }

    @Test
    void repeatedPositionThreeTimesEndsInDraw() {
        GameLogic game = emptyGame();
        Board board = game.getBoard();

        board.setPiece(square(7, 0), new Piece(Piece.red_king));
        board.setPiece(square(0, 1), new Piece(Piece.black_king));

        // Complete two cycles, then repeat the first position a third time.
        for (int cycle = 0; cycle < 2; cycle++) {
            assertTrue(game.move(square(7, 0), square(6, 1)));
            assertTrue(game.move(square(0, 1), square(1, 0)));
            assertTrue(game.move(square(6, 1), square(7, 0)));
            assertTrue(game.move(square(1, 0), square(0, 1)));
        }

        // This move creates the third occurrence and immediately ends the game.
        assertTrue(game.move(square(7, 0), square(6, 1)));

        assertEquals(GameLogic.GameState.DRAW, game.getGameState());
        assertEquals("The game is a tie.", game.getGameResultMessage());
    }

    // Creates an empty board so each test can arrange only the pieces it needs.
    private GameLogic emptyGame() {
        GameLogic game = new GameLogic();
        game.getBoard().clear();
        return game;
    }

    // Converts a board row and column into the game's square index.
    private int square(int row, int column) {
        return Board.toIndex(row, column);
    }
}