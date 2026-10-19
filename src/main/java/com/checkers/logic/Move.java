package main.java.com.checkers.logic;

// Represents a move in the checkers game, including the start and end squares, and provides methods to determine if the move is a jump and to get the middle square in a jump move.
// Provides methods to get the start and end squares of the move.
// Provides a method to determine if the move is a jump and a method to get the middle square in a jump move.
public class Move {
    private int start;
    private int end;

    // Constructs a new Move with the specified start and end squares.
    public Move(int start, int end) {
        this.start = start;
        this.end = end;
    }

    // Returns the start square of the move.
    public int getStart() {
        return start;
    }

    // Returns the end square of the move.
    public int getEnd() {
        return end;
    }

    // Returns true if the move is a jump (i.e., the piece moves two squares), false otherwise.
    public boolean isJump() {
        int startRow = Board.getRow(start);
        int startCol = Board.getCol(start);

        int endRow = Board.getRow(end);
        int endCol = Board.getCol(end);

        return Math.abs(startRow - endRow) == 2 && Math.abs(startCol - endCol) == 2;
    }

    // Returns the square jumped over in a jump move.
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
