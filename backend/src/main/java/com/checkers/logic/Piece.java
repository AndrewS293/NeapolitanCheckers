package com.checkers.logic;


// Represents a piece in the checkers game, including its type (empty, red, black, red king, black king) and provides methods to check its properties.
// Provides methods to determine if the piece is empty, red, black, or a king, and a method to promote a piece to a king.
public class Piece {
    public static final int empty = 0;
    public static final int red = 1;
    public static final int black = 2;

    public static final int red_king = 3;
    public static final int black_king = 4;

    // The type of the piece, one of the constants defined above.
    private int type;

    // Constructs a new Piece with the specified type.
    public Piece(int type) {
        this.type = type;
    }

    // Returns the type of the piece.
    public int getType() {
        return type;
    }

    // Sets the type of the piece.
    public void setType(int type) {
        this.type = type;
    }

    // Returns true if the piece is empty, false otherwise.
    public boolean isEmpty() {
        return type == empty;
    }

    // Returns true if the piece is red (either a regular red piece or a red king), false otherwise.
    public boolean isRed() {
        return type == red || type == red_king;
    }

    // Returns true if the piece is black (either a regular black piece or a black king), false otherwise.
    public boolean isBlack() {
        return type == black || type == black_king;
    }

    // Returns true if the piece is a king (either a red king or a black king), false otherwise.
    public boolean isKing() {
        return type == red_king || type == black_king;
    }

    // Promotes the piece to a king if it is a regular red or black piece.
    public void makeKing() {
        if (type == red) {
            type = red_king;
        } else if (type == black) {
            type = black_king;
        }
    }
}
