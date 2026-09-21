package main.java.com.checkers.logic;

public class GameLogic {
    public static final int red_player = Piece.red;
    public static final int black_player = Piece.black;

    private Board board;
    private int currentPlayer;

    public GameLogic() {
        board = new Board();
        currentPlayer = red_player; // red player starts first
    }

    public Board getBoard() {
        return board;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    // switch to the other player's turn
    public void switchPlayer() {
        if (currentPlayer == red_player) {
            currentPlayer = black_player;
        } else {
            currentPlayer = red_player;
        }
    }

    // check if a piece at a given index belongs to the current player
    public boolean isCurrentPlayerPiece(int index) {
        Piece piece = board.getPiece(index);
        if (piece == null || piece.isEmpty()) {
            return false;
        }
        if (currentPlayer == red_player) {
            return piece.isRed();
        } else {
            return piece.isBlack();
        }
    }

    // attempt to make a move
    public boolean move(int start, int end) {
        if (!board.isValidIndex(start) || !board.isValidIndex(end)) {
            return false; // invalid indices
        }

        Piece piece = board.getPiece(start);
        if (piece == null || piece.isEmpty() || !isCurrentPlayerPiece(start)) {
            return false; // no piece to move or not current player's piece
        }

        if (!board.isEmpty(end)) {
            return false; // destination square is not empty
        }

        if (!isValidMove(new Move(start, end), piece)) {
            return false; // move is not valid
        }

        Move move = new Move(start, end);
        
        // move the piece
        board.setPiece(end, piece);
        board.removePiece(start);

        // if move is a jump, remove the jumped piece
        if (move.isJump()) {
            board.removePiece(move.getMiddleSquare());
        }

        // check if king
        promotePiece(end);

        // switch to the other player's turn
        switchPlayer();
        return true;
    }

    // check if normal move or jump is valid for the piece
    public boolean isValidMove(int start, int end) {
        Piece piece = board.getPiece(start);
        int startRow = Board.getRow(start);
        int startCol = Board.getCol(start);

        int endRow = Board.getRow(end);
        int endCol = Board.getCol(end);

        int rowDiff = endRow - startRow;
        int colDiff = Math.abs(endCol - startCol);

        // normal move: one square diagonally forward
        if (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 1) {
            //king can move in any direction, regular pieces can only move forward
            if (piece.isKing()) {
                return true;
            }

            // red moves upward
            if (piece.isRed()) {
                return rowDiff == -1;
            }

            //black moves downward
            if (piece.isBlack()) {
                return rowDiff == 1;
            }
        }

        // jump move: two squares diagonally over an opponent's piece
        if (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 2) {
            Move move = new Move(start, end);
            int middleSquare = move.getMiddleSquare();
            Piece jumpedPiece = board.getPiece(middleSquare);
            if (jumpedPiece == null || jumpedPiece.isEmpty()) {
                return false; // no piece to jump over
            }

            // cannot jump to your own piece
            if (piece.isRed() && jumpedPiece.isRed()) {
                return false;
            }

            if (piece.isBlack() && jumpedPiece.isBlack()) {
                return false;
            }

            // king can jump any direction
            if (piece.isKing()) {
                return true;
            }

            // red can only jump upward
            if (piece.isRed()) {
                return rowDiff == -2;
            }

            // black can only jump downward
            if (piece.isBlack()) {
                return rowDiff == 2;
            }
        }
        return false;
    }

    // promote a piece to king if it reaches the opposite end of the board
    private void promotePiece(int index) {
        Piece piece = board.getPiece(index);
        int row = Board.getRow(index);

        if (piece.isRed() && row == 0) {
            piece.makeKing();
        } else if (piece.isBlack() && row == Board.board_size - 1) {
            piece.makeKing();
        }
    }
}
