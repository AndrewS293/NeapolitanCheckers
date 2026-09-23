package com.checkers.logic;

import com.checkers.logic.Piece;


public class Board {
    public static final int board_size = 8;
    public static final int playable_squares = 32;

    private Piece[] squares;

    public Board() {
        squares = new Piece[playable_squares];
        reset();
    }

    // set the board to the starting position for a new game
    public void reset() {
        for (int i = 0; i < playable_squares; i++) {
            squares[i] = new Piece(Piece.empty);
        }
        // set up the black pieces
        for (int i = 0; i < 12; i++) {
            squares[i] = new Piece(Piece.black);
        }
        // set up the red pieces
        for (int i = 20; i < playable_squares; i++) {
            squares[i] = new Piece(Piece.red);
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
        return index >= 0 && index < playable_squares;
    }

    // convert board row and column to playable square index
    public static int toIndex(int row, int col) {
        if (row < 0 || row >= board_size || col < 0 || col >= board_size) {
            return -1;
        }
        if ((row + col) % 2 == 0) {
            return -1; // not a playable square
        }
        return (row * (board_size / 2)) + (col / 2);
    }

    // get the row from a playable square index
    public static int getRow(int index) {
        if (index < 0 || index >= playable_squares) {
            return -1;
        }
        return index / (board_size / 2);
    }

    // get the column from a playable square index
    public static int getCol(int index) {
        if (index < 0 || index >= playable_squares) {
            return -1;
        }
        int row = getRow(index);
        return (index % (board_size / 2)) * 2 + ((row + 1) % 2);
    }

    // removes a piece from the board
    public void removePiece(int index) {
        if (!isValidIndex(index)) {
            return;
        }
        squares[index] = new Piece(Piece.empty);
    }
}
