package com.checkers.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps the checkers rules separate from the JavaFX screen.
public class GameLogic {

    public static final int red_player = Piece.red;
    public static final int black_player = Piece.black;

    // A tie occurs after 80 completed turns without a capture or promotion.
    public static final int DRAW_TURN_LIMIT = 80;

    // Stores the possible states of the game.
    public enum GameState {
        IN_PROGRESS,
        RED_WON,
        BLACK_WON,
        DRAW
    }

    private final Board board = new Board();

    // Counts repeated board positions for the tie rule.
    private final Map<String, Integer> positionCounts = new HashMap<>();

    private int currentPlayer = red_player;

    // Stores the piece that must continue jumping during a multijump.
    // A value of -1 means that no multijump is currently happening.
    private int forcedJumpIndex = -1;

    private int turnsWithoutProgress = 0;
    private GameState gameState = GameState.IN_PROGRESS;

    public GameLogic() {
        recordCurrentPosition();
    }

    public Board getBoard() {
        return board;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    // Returns the current state of the game.
    public GameState getGameState() {
        return gameState;
    }

    // Checks whether the game has ended.
    public boolean isGameOver() {
        return gameState != GameState.IN_PROGRESS;
    }

    // Returns the piece that must continue jumping.
    public int getForcedJumpIndex() {
        return forcedJumpIndex;
    }

    // Tells the GUI whether a multijump is happening.
    public boolean isMultiJumpInProgress() {
        return forcedJumpIndex != -1;
    }

    // Returns the number of turns without a capture or promotion.
    public int getTurnsWithoutProgress() {
        return turnsWithoutProgress;
    }

    // Creates the message that will appear when the game ends.
    public String getGameResultMessage() {
        return switch (gameState) {
            case RED_WON -> "Red wins!";
            case BLACK_WON -> "Black wins!";
            case DRAW -> "The game is a tie.";
            case IN_PROGRESS -> "Game in progress";
        };
    }

    // Switches to the other player after a completed turn.
    public void switchPlayer() {
        currentPlayer = otherPlayer(currentPlayer);
    }

    // Checks whether the selected piece belongs to the current player.
    public boolean isCurrentPlayerPiece(int index) {
        return belongsToPlayer(board.getPiece(index), currentPlayer);
    }

    // Attempts to complete one move.
    public boolean move(int start, int end) {

        // Do not allow moves after the game ends or moves that break the rules.
        if (isGameOver() || !isValidMove(start, end)) {
            return false;
        }

        Piece piece = board.getPiece(start);
        Move move = new Move(start, end);

        // Remember whether the piece was already a king.
        boolean wasKing = piece.isKing();

        // Move the selected piece to its destination.
        board.setPiece(end, piece);
        board.removePiece(start);

        // Remove the opponent's piece when this move is a jump.
        if (move.isJump()) {
            board.removePiece(move.getMiddleSquare());
        }

        promotePiece(end);

        // Check whether this move caused the piece to become a king.
        boolean promotedThisMove = !wasKing && piece.isKing();

        // Keep the same player's turn if another jump is available.
        if (move.isJump() && !promotedThisMove && hasJumpFrom(end)) {
            forcedJumpIndex = end;
            return true;
        }

        // The move or multijump is finished.
        forcedJumpIndex = -1;

        // Captures and promotions reset the tie counter.
        if (move.isJump() || promotedThisMove) {
            turnsWithoutProgress = 0;
        } else {
            turnsWithoutProgress++;
        }

        switchPlayer();
        updateGameStateAfterCompletedTurn();

        return true;
    }

    // Checks ownership, forced jumps, multijumps, and basic move rules.
    public boolean isValidMove(int start, int end) {

        // Basic checks that must pass before examining the move.
        if (isGameOver()
                || !board.isValidIndex(start)
                || !board.isValidIndex(end)
                || !board.isEmpty(end)
                || !isCurrentPlayerPiece(start)) {
            return false;
        }

        Move candidate = new Move(start, end);

        // During a multijump, the player must continue using the same piece.
        if (forcedJumpIndex != -1 && start != forcedJumpIndex) {
            return false;
        }

        // If any piece can jump, the player cannot make a normal move.
        if (hasAnyJump(currentPlayer) && !candidate.isJump()) {
            return false;
        }

        // A multijump must continue with another jump.
        if (forcedJumpIndex != -1 && !candidate.isJump()) {
            return false;
        }

        return isValidMoveIgnoringForcedJump(start, end);
    }

    // Returns every legal destination for the selected piece.
    // The GUI will use this list to highlight possible moves.
    public List<Integer> getLegalDestinations(int start) {
        List<Integer> destinations = new ArrayList<>();

        if (!isCurrentPlayerPiece(start) || isGameOver()) {
            return destinations;
        }

        for (int end = 0; end < Board.playable_squares; end++) {
            if (isValidMove(start, end)) {
                destinations.add(end);
            }
        }

        return destinations;
    }

    // Checks whether a player has at least one legal move.
    public boolean hasAnyLegalMove(int player) {
        boolean jumpRequired = hasAnyJump(player);

        for (int start = 0; start < Board.playable_squares; start++) {
            if (!belongsToPlayer(board.getPiece(start), player)) {
                continue;
            }

            for (int end = 0; end < Board.playable_squares; end++) {
                if (isLegalForPlayer(start, end, player, jumpRequired)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Checks whether any piece belonging to this player can jump.
    public boolean hasAnyJump(int player) {
        for (int index = 0; index < Board.playable_squares; index++) {
            if (belongsToPlayer(board.getPiece(index), player)
                    && hasJumpFrom(index)) {
                return true;
            }
        }

        return false;
    }

    // Counts how many pieces a player has remaining.
    public int countPieces(int player) {
        int count = 0;

        for (int index = 0; index < Board.playable_squares; index++) {
            if (belongsToPlayer(board.getPiece(index), player)) {
                count++;
            }
        }

        return count;
    }

    // Checks the shape and direction of a move.
    // Forced-jump rules are checked separately.
    private boolean isValidMoveIgnoringForcedJump(int start, int end) {
        Piece piece = board.getPiece(start);

        int rowDiff = Board.getRow(end) - Board.getRow(start);
        int colDiff = Math.abs(Board.getCol(end) - Board.getCol(start));

        // A normal move travels one row and one column diagonally.
        if (Math.abs(rowDiff) == 1 && colDiff == 1) {
            return movesInAllowedDirection(piece, rowDiff, 1);
        }

        // A jump travels two rows and two columns diagonally.
        if (Math.abs(rowDiff) == 2 && colDiff == 2) {
            Move move = new Move(start, end);
            Piece jumpedPiece = board.getPiece(move.getMiddleSquare());

            return isOpponent(piece, jumpedPiece)
                    && movesInAllowedDirection(piece, rowDiff, 2);
        }

        return false;
    }

    // Checks whether a move is legal for a specific player.
    // This is used when checking whether the player has any moves left.
    private boolean isLegalForPlayer(
            int start,
            int end,
            int player,
            boolean jumpRequired) {

        if (!board.isValidIndex(start)
                || !board.isValidIndex(end)
                || !board.isEmpty(end)
                || !belongsToPlayer(board.getPiece(start), player)) {
            return false;
        }

        Move candidate = new Move(start, end);

        return (!jumpRequired || candidate.isJump())
                && isValidMoveIgnoringForcedJump(start, end);
    }

    // Checks whether one specific piece has another available jump.
    private boolean hasJumpFrom(int start) {
        if (!board.isValidIndex(start) || board.isEmpty(start)) {
            return false;
        }

        int row = Board.getRow(start);
        int col = Board.getCol(start);

        // These are the four possible diagonal jump directions.
        int[][] directions = {
                { -2, -2 },
                { -2, 2 },
                { 2, -2 },
                { 2, 2 }
        };

        for (int[] direction : directions) {
            int endRow = row + direction[0];
            int endCol = col + direction[1];
            int end = Board.toIndex(endRow, endCol);

            if (end != -1
                    && board.isEmpty(end)
                    && isValidMoveIgnoringForcedJump(start, end)) {
                return true;
            }
        }

        return false;
    }

    // Checks whether a piece is moving in a direction it is allowed to move.
    private boolean movesInAllowedDirection(
            Piece piece,
            int rowDiff,
            int distance) {

        if (piece == null || piece.isEmpty()) {
            return false;
        }

        // Kings can move forward or backward.
        if (piece.isKing()) {
            return Math.abs(rowDiff) == distance;
        }

        // Regular red pieces move upward and regular black pieces move downward.
        if (piece.isRed()) {
            return rowDiff == -distance;
        }

        return rowDiff == distance;
    }

    // Checks whether two pieces belong to opposing players.
    private boolean isOpponent(Piece first, Piece second) {
        return first != null
                && second != null
                && !second.isEmpty()
                && ((first.isRed() && second.isBlack())
                        || (first.isBlack() && second.isRed()));
    }

    // Checks whether a piece belongs to the requested player.
    private boolean belongsToPlayer(Piece piece, int player) {
        if (piece == null || piece.isEmpty()) {
            return false;
        }

        if (player == red_player) {
            return piece.isRed();
        }

        return piece.isBlack();
    }

    // Returns the player who is not currently taking the turn.
    private int otherPlayer(int player) {
        if (player == red_player) {
            return black_player;
        }

        return red_player;
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

    // Checks for a winner or tie after a player's completed turn.
    private void updateGameStateAfterCompletedTurn() {
        int playerToMove = currentPlayer;
        int previousPlayer = otherPlayer(playerToMove);

        // A player loses if they have no pieces or cannot make a legal move.
        if (countPieces(playerToMove) == 0
                || !hasAnyLegalMove(playerToMove)) {

            if (previousPlayer == red_player) {
                gameState = GameState.RED_WON;
            } else {
                gameState = GameState.BLACK_WON;
            }

            return;
        }

        // The game is a tie after too many turns without a capture or promotion.
        if (turnsWithoutProgress >= DRAW_TURN_LIMIT) {
            gameState = GameState.DRAW;
            return;
        }

        // The game is also a tie if the same position occurs three times.
        int repetitions = positionCounts.merge(
                createPositionKey(),
                1,
                Integer::sum);

        if (repetitions >= 3) {
            gameState = GameState.DRAW;
        }
    }

    // Records the starting board position.
    private void recordCurrentPosition() {
        positionCounts.merge(
                createPositionKey(),
                1,
                Integer::sum);
    }

    // Creates a unique description of the board and current player.
    private String createPositionKey() {
        StringBuilder key = new StringBuilder();

        key.append(currentPlayer).append(":");

        for (int index = 0; index < Board.playable_squares; index++) {
            key.append(board.getPiece(index).getType());
        }

        return key.toString();
    }
}
