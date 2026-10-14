package com.checkers.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {


    //A.C. 15.1 Multi-Jump 15.2 Multi-Jump Ends 15.3 Multi-Jump Pieces Removed
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

    
    //18.1 Win When Opponent Has No Pieces 19.1 Lose When I Have No Pieces
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


    //18.2 Win When Opponent Still Has Pieces But Cannot Move 19.2 Lose When I Have Pieces But Cannot Move
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

    //20.1 Draw When Position Repeats Three Times //20.2 Notify Players Of Draw
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


    private GameLogic newGameWithEmptyBoard() {
        GameLogic game = new GameLogic();
        for (int index = 0; index < Board.playable_squares; index++) {
            game.getBoard().removePiece(index);
        }
        return game;
    }

    //13.1 Basic Move Successful
    @Test
    void regularPieceMovesOneDiagonalStepForward() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int start = square(5, 0);
        int end = square(4, 1);
        Piece red = new Piece(Piece.red);
        board.setPiece(start, red);

        assertTrue(game.move(start, end));
        assertSame(red, board.getPiece(end));
        assertTrue(board.isEmpty(start));
        assertEquals(GameLogic.black_player, game.getCurrentPlayer());
    }

    //13.2 Basic Move Unsuccessful
    @Test
    void moveIsRejectedWhenDestinationIsOccupied() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int start = square(5, 0);
        int end = square(4, 1);
        Piece red = new Piece(Piece.red);
        Piece black = new Piece(Piece.black);
        board.setPiece(start, red);
        board.setPiece(end, black);

        assertFalse(game.move(start, end));
        assertSame(red, board.getPiece(start));
        assertSame(black, board.getPiece(end));
    }


    //14.1 Basic Hop Successful //14.3 Capture piece removed
    @Test
    void jumpOverOpponentWithEmptyLandingSquareSucceedsAndCaptures() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int start = square(5, 0);
        int captured = square(4, 1);
        int end = square(3, 2);
        Piece red = new Piece(Piece.red);
        board.setPiece(start, red);
        board.setPiece(captured, new Piece(Piece.black));

        assertTrue(game.move(start, end));
        assertSame(red, board.getPiece(end));
        assertTrue(board.isEmpty(start));
        assertTrue(board.isEmpty(captured));
    }

    //14.2 Hop Unsuccessful 
    @Test
    void jumpIsRejectedWhenLandingSquareIsOccupied() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int start = square(5, 0);
        int captured = square(4, 1);
        int end = square(3, 2);
        Piece red = new Piece(Piece.red);
        Piece landingPiece = new Piece(Piece.black);
        board.setPiece(start, red);
        board.setPiece(captured, new Piece(Piece.black));
        board.setPiece(end, landingPiece);

        assertFalse(game.move(start, end));
        assertSame(red, board.getPiece(start));
        assertFalse(board.isEmpty(captured));
        assertSame(landingPiece, board.getPiece(end));
    }

    //14.4 Normal Move Rejected When Current Player Has Available Capture
    @Test
    void normalMoveIsRejectedWhenCurrentPlayerHasAvailableCapture() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int capturingPiece = square(5, 0);
        int opponentPiece = square(4, 1);
        int captureLanding = square(3, 2);
        int otherPiece = square(5, 4);
        int normalDestination = square(4, 5);
        board.setPiece(capturingPiece, new Piece(Piece.red));
        board.setPiece(opponentPiece, new Piece(Piece.black));
        Piece otherRed = new Piece(Piece.red);
        board.setPiece(otherPiece, otherRed);

        assertFalse(game.move(otherPiece, normalDestination));
        assertSame(otherRed, board.getPiece(otherPiece));
        assertTrue(board.isEmpty(normalDestination));
        assertTrue(board.isEmpty(captureLanding));
    }


    //16.1 Piece Promoted to King
    @Test
    void redPieceIsPromotedWhenItReachesOpponentsBackRow() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int start = square(1, 2);
        int end = square(0, 3);
        board.setPiece(start, new Piece(Piece.red));

        assertTrue(game.move(start, end));
        assertEquals(Piece.red_king, board.getPiece(end).getType());
        assertTrue(board.isEmpty(start));
    }

    @Test
    void blackPieceIsPromotedWhenItReachesOpponentsBackRow() {
        GameLogic game = newGameWithEmptyBoard();
        game.switchPlayer();
        int start = square(6, 1);
        int end = square(7, 2);
        game.getBoard().setPiece(start, new Piece(Piece.black));

        assertTrue(game.move(start, end));
        assertEquals(Piece.black_king, game.getBoard().getPiece(end).getType());
    }


    //17.1 King Can Move Diagonally Forward and Backward
    @Test
    void kingCanMoveDiagonallyForwardAndBackward() {
        GameLogic forwardGame = newGameWithEmptyBoard();
        int start = square(4, 3);
        int forward = square(3, 2);
        Piece forwardKing = new Piece(Piece.red_king);
        forwardGame.getBoard().setPiece(start, forwardKing);

        assertTrue(forwardGame.move(start, forward));
        assertSame(forwardKing, forwardGame.getBoard().getPiece(forward));

        GameLogic backwardGame = newGameWithEmptyBoard();
        int backwardStart = square(4, 3);
        int backward = square(5, 4);
        Piece backwardKing = new Piece(Piece.red_king);
        backwardGame.getBoard().setPiece(backwardStart, backwardKing);

        assertTrue(backwardGame.move(backwardStart, backward));
        assertSame(backwardKing, backwardGame.getBoard().getPiece(backward));
    }


    //17.2 King Can Capture Opponent Diagonally
    @Test
    void kingCanCaptureOpponentDiagonally() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int start = square(4, 3);
        int captured = square(5, 4);
        int end = square(6, 5);
        Piece king = new Piece(Piece.red_king);
        board.setPiece(start, king);
        board.setPiece(captured, new Piece(Piece.black));

        assertTrue(game.move(start, end));
        assertSame(king, board.getPiece(end));
        assertTrue(board.isEmpty(captured));
    }


    //17.3 King Cannot Move to Non-Diagonal Square
    @Test
    void kingCannotMoveToNonDiagonalSquare() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int start = square(4, 3);
        int nonDiagonal = square(4, 5);
        Piece king = new Piece(Piece.red_king);
        board.setPiece(start, king);

        assertFalse(game.move(start, nonDiagonal));
        assertSame(king, board.getPiece(start));
        assertTrue(board.isEmpty(nonDiagonal));
    }

    @Test
    void moveIsRejectedForInvalidIndicesAndWrongPlayer() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        board.setPiece(square(2, 1), new Piece(Piece.black));
        board.setPiece(square(5, 0), new Piece(Piece.red));

        assertFalse(game.move(-1, square(4, 0)));
        assertFalse(game.move(square(2, 1), square(3, 0)));
    }

    @Test
    void isCurrentPlayerPieceMatchesTurnAndOccupiedSquare() {
        GameLogic game = newGameWithEmptyBoard();
        Board board = game.getBoard();
        int redSquare = square(5, 0);
        int blackSquare = square(2, 1);
        board.setPiece(redSquare, new Piece(Piece.red));
        board.setPiece(blackSquare, new Piece(Piece.black));

        assertTrue(game.isCurrentPlayerPiece(redSquare));
        assertFalse(game.isCurrentPlayerPiece(blackSquare));
        assertFalse(game.isCurrentPlayerPiece(square(4, 1)));
    }
}