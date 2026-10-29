package com.checkers.logic;
import java.util.List;


import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Board;


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

    public boolean isJump(Board board) {
        int dr = board.rowOf(end) - board.rowOf(start);
        int dc = board.colOf(end) - board.colOf(start);
        return Math.abs(dr) == 2 && Math.abs(dc) == 2;
    }

    public int getMiddleSquare(Board board) {
        int row = (board.rowOf(start) + board.rowOf(end)) / 2;
        int col = (board.colOf(start) + board.colOf(end)) / 2;
        return board.indexAt(row, col);
    }




    public record TurnMove(List<MoveStep> steps) {
        public TurnMove {
            if (steps == null || steps.isEmpty()) {
                throw new IllegalArgumentException(
                    "A turn must contain at least one step");
            }

            steps = List.copyOf(steps);
        }

        public boolean isCapture() {
            return steps.stream().anyMatch(MoveStep::isCapture);
        }

        public int getStartIndex() {
            return steps.getFirst().startIndex();
        }

        public int getEndIndex() {
            return steps.getLast().endIndex();
        }

        public long getCapturedCount() {
            return steps.stream()
                    .filter(MoveStep::isCapture)
                    .count();
        }
    }

    public record MoveStep(
        int startIndex,
        int endIndex,
        int capturedIndex) {

        public boolean isCapture() {
            return capturedIndex >= 0;
        }
    }

}