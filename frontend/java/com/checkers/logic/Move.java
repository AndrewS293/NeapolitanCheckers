package com.checkers.logic;
import com.checkers.logic.Board;


public class Move {
    private int start;
    private int end;

    public Move(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    // determine if the move is a jump (i.e., the piece moves two squares)
    public boolean isJump() {
        int startRow = Board.getRow(start);
        int startCol = Board.getCol(start);

        int endRow = Board.getRow(end);
        int endCol = Board.getCol(end);

        return Math.abs(startRow - endRow) == 2 && Math.abs(startCol - endCol) == 2;
    }

    // gets the square jumped over in a jump move
    public int getMiddleSquare() {
        int startRow = Board.getRow(start);
        int startCol = Board.getCol(start);

        int endRow = Board.getRow(end);
        int endCol = Board.getCol(end);

        int middleRow = (startRow + endRow) / 2;
        int middleCol = (startCol + endCol) / 2;
        return Board.toIndex(middleRow, middleCol);
    }
}
