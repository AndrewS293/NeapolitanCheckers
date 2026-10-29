package com.checkers.logic;

import com.checkers.config.GameConfig;

public class Board {
    @Deprecated public static final int board_size = 8;
    @Deprecated public static final int playable_squares = 32;

    private final GameConfig config;
    private final Piece[] squares;

    public Board() {
        this(new GameConfig(
                "AMERICAN", 8, 3, 12,
                GameConfig.SquareLayout.DARK_SQUARES_ONLY, true));
    }

    public Board(GameConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("GameConfig cannot be null");
        }
        this.config = config;
        this.squares = new Piece[config.getCellCount()];
        clear();
        reset();
    }

    // set the board to the starting position for a new game
    public void reset() {
        clear();

        int size = config.getBoardSize();
        int rows = config.getStartingRowsPerPlayer();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (!config.isPlayableSquare(row, col)) {
                    continue;
                }

                int index = indexAt(row, col);

                if (row < rows) {
                    setPiece(index, new Piece(Piece.black));
                } else if (row >= size - rows) {
                    setPiece(index, new Piece(Piece.red));
                }
            }
        }
    }


    // Removes all pieces from the board.
    // This helps us create specific board situations for testing.
    public void clear() {
        for (int i = 0; i < squares.length; i++) {
            squares[i] = new Piece(Piece.empty);
        }
    }

    


    // get the piece at a playable square
    public Piece getPiece(int index) {
        if (!isValidIndex(index)) {
            return null;
        }
        return squares[index];
    }

    // place a piece at a playable square
    public void setPiece(int index, Piece piece) {
        if (!isValidIndex(index)) {
            return;
        }
        squares[index] = piece;
    }

    // check if square is empty
    public boolean isEmpty(int index) {
        if (!isValidIndex(index)) {
            return false;
        }
        return squares[index].isEmpty();
    }

    // check if index is one of the playable squares
    
    public boolean isValidIndex(int index) {
        return index >= 0 && index < squares.length;
    }

    public Board copy() {
        Board copy = new Board(config);

        for (int index = 0; index < squares.length; index++) {
            Piece piece = squares[index];
            copy.setPiece(
                index,
                piece == null
                    ? new Piece(Piece.empty)
                    : new Piece(piece.getType())
            );
        }

        return copy;
    }



    //removes a piece from the board
    public void removePiece(int index) {
        if (!isValidIndex(index)) {
            return;
        }
        squares[index] = new Piece(Piece.empty);
    }


    

    public GameConfig getConfig() {
        return config;
    }

    public int getCellCount() {
        return squares.length;
    }

    public int getBoardSize() {
        return config.getBoardSize();
    }

    
    public boolean isPlayableIndex(int index) {
        if (!isValidIndex(index)) {
            return false;
        }
        return config.isPlayableSquare(rowOf(index), colOf(index));
    }

    public int indexAt(int row, int col) {
        return config.toIndex(row, col);
    }

    public boolean isValidCoordinate(int row, int col) { 
        return config.isValidCoordinate(row, col); 
    }

    public int rowOf(int index) {
        return config.getRow(index);
    }

    public int colOf(int index) {
        return config.getCol(index);
    }



    /**
     * Legacy helpers for the original American 32-playable-square indexing.
     * New engine code should use indexAt/rowOf/colOf instead.
     */
    @Deprecated
    public static int toIndex(int row, int col) {
        if (row < 0 || row >= board_size || col < 0 || col >= board_size
                || (row + col) % 2 == 0) {
            return -1;
        }
        return row * (board_size / 2) + col / 2;
    }

    @Deprecated
    public static int getRow(int index) {
        if (index < 0 || index >= playable_squares) return -1;
        return index / (board_size / 2);
    }

    @Deprecated
    public static int getCol(int index) {
        if (index < 0 || index >= playable_squares) return -1;
        int row = getRow(index);
        return (index % (board_size / 2)) * 2 + ((row + 1) % 2);
    }
}
