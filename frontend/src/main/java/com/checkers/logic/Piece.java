package com.checkers.logic;


public class Piece {
    public static final int empty = 0;
    public static final int red = 1;
    public static final int black = 2;

    public static final int red_king = 3;
    public static final int black_king = 4;

    private int type;

    public Piece(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isEmpty() {
        return type == empty;
    }

    public boolean isRed() {
        return type == red || type == red_king;
    }

    public boolean isBlack() {
        return type == black || type == black_king;
    }

    public boolean isKing() {
        return type == red_king || type == black_king;
    }

    public void makeKing() {
        if (type == red) {
            type = red_king;
        } else if (type == black) {
            type = black_king;
        }
    }
}
