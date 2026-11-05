package main.java.com.checkers.variants.thai;

import com.checkers.config.GameConfig;
import com.checkers.logic.Board;
import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Move.TurnMove;
import com.checkers.logic.Piece;
import com.checkers.rules.GameRules;

import java.util.*;

// implements rules for Thai Checkers Variant
public final class ThaiRules implements GameRules {
    // represents all four diagonal movement directions
    private static final int[][] DIAGONALS = {
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    // thai checkers uses 8x8 board with 8 pieces per player
    private final GameConfig config = new GameConfig("THAI", 8, 2, 8,
    GameConfig.SquareLayout.DARK_SQUARES_ONLY, true);

    // returns unique ID to identify this variant
    @Override 
    public String getVariantId() {
        return "THAI";
    }

    // returns name displayed to player
    @Override public String getDisplayName() {
        return "Thai Checkers";
    }

    // returns configuration for thai checkres board
    @Override 
    public GameConfig getConfig() {
        return config;
    }

    // initializes board after checking it belongs to this variant
    @Override
    public void initializeBoard(Board board) {
        // implementation for initializing Thai Checkers board
        if (board == null || !getVariantId().equals(board.getVariantId())) {
            throw new IllegalArgumentException("Wrong board");
        }
        board.reset();
    }

    // generates all legal turns for selected player
    // captures are mandatory, but players don't have to choose the
    // capture sequence that takes greatest num of pieces
    @Override 
    public List<TurnMove> getLegalTurns(Board board, int player) {
        validateBoardAndPlayer(board, player);

        // search for all possible capture sequences first
        List<TurnMove> captures = generateAllCaptures(board, player);

        // if a capture is available, only capture moves are legal
        if (!captures.isEmpty()) {
            return List.copyOf(captures);
        }

        // if no captures are available, generate normal moves
        return generateQuietTurns(board, player);
    }

    // finds every possible capture sequence for selected player
        private List<TurnMove> generateAllCaptures(Board board, int player) {
        List<TurnMove> captures = new ArrayList<>();

        // checks every playable square for a piece belonging to player
          for (int start = 0; start < board.getCellCount(); start++) {
            if (!board.isPlayableIndex(start)) {
                continue;
            }

            if (!belongsTo(board.getPiece(start), player)) {
                continue;
            }

            // recursively searches for additional captures using this piece
            findCaptures(board, start, player, new ArrayList<>(), captures);
        }
        return captures;
    }

    // recursively generates complete multiple-capture turns
    // same piece must continue capturing whenever another capture exists
    private void findCaptures(Board position, int current, int player, 
        List<MoveStep> path, List<TurnMove> output) {
            Piece movingPiece = position.getPiece(current);
            if (movingPiece == null || movingPiece.isEmpty()) {
            return;
        }

        // a regular piece becomes a king when it reaches the opposite
        // back row during a capture - its capture sequence ends immediately
         if (!movingPiece.isKing() && !path.isEmpty()
                && isPromotionRow(position, current, movingPiece)) {
            output.add(new TurnMove(path));
            return;
        }

        // finds all captures available from the piece's current position
        List<CaptureOption> options = getCaptureOptions(position, current, movingPiece, player);

        // if no additional captures exist, save the completed turn
         if (options.isEmpty()) {
            if (!path.isEmpty()) {
                output.add(new TurnMove(path));
            }
            return;
        }

        // explore every availabel capture direction
         for (CaptureOption option : options) {

            // copy the board so each possible capture sequence
            // can be explored without changing the original board
            Board next = position.copy();

            Piece nextPiece = next.getPiece(current);

            // move the piece to the landing square
            next.setPiece(option.landingIndex(), nextPiece);
            next.removePiece(current);

            // thai rules remove captured pieces immediately
            next.removePiece(option.capturedIndex());

            // add this capture to the current sequence
            List<MoveStep> nextPath = new ArrayList<>(path);
            nextPath.add(new MoveStep(current, option.landingIndex(), option.capturedIndex()));

            // continue searching for more captures from the new position
            findCaptures(next, option.landingIndex(), player, nextPath, output);
        }
    }

    // finds possible capture moves for a regular piece or king
    private List<CaptureOption> getCaptureOptions(
            Board board, int current, Piece piece, int player) {

        List<CaptureOption> options = new ArrayList<>();

        int row = board.rowOf(current);
        int col = board.colOf(current);

        // regular pieces can only capture diagonally forward.
        if (!piece.isKing()) {
            for (int[] d : DIAGONALS) {

                // ignore directions that move backward
                if (!isForward(piece, d[0])) {
                    continue;
                }

                // finds the opponent's square and the square beyond it
                int middle = board.indexAt(row + d[0], col + d[1]);
                int landing = board.indexAt(row + 2 * d[0], col + 2 * d[1]
                );

                // both squares must be playable and the landing square empty
                if (middle < 0 || landing < 0
                        || !board.isPlayableIndex(middle)
                        || !board.isPlayableIndex(landing)
                        || !board.isEmpty(landing)) {
                    continue;
                }

                // middle square must contain an opponent's piece
                if (isOpponent(board.getPiece(middle), player)) {
                    options.add(new CaptureOption(middle, landing));
                }
            }

            return options;
        }

        // thai kings can capture in any diagonal direction, but must land on first square
        // immediately after captured piece
          for (int[] d : DIAGONALS) {

            int scanRow = row + d[0];
            int scanCol = col + d[1];

            // search along the diagonal until an occupied square is found
            while (board.isValidCoordinate(scanRow, scanCol)) {

                int index = board.indexAt(scanRow, scanCol);

                if (!board.isPlayableIndex(index)) {
                    break;
                }

                Piece occupant = board.getPiece(index);

                // continue scanning through empty squares
                if (occupant == null || occupant.isEmpty()) {
                    scanRow += d[0];
                    scanCol += d[1];
                    continue;
                }

                // only the first occupied square can be captured
                // friendly pieces block the diagonal
                if (!isOpponent(occupant, player)) {
                    break;
                }

                // thai kings must land immediately after the opponent
                int landing = board.indexAt(
                        scanRow + d[0],
                        scanCol + d[1]
                );

                // immediate landing square must be playable and empty
                if (landing >= 0
                        && board.isPlayableIndex(landing)
                        && board.isEmpty(landing)) {
                    options.add(new CaptureOption(index, landing));
                }

                // stop after the first occupied square
                break;
            }
        }

        return options;
    }

    // generates all normal moves when no capture is available
    private List<TurnMove> generateQuietTurns(Board board, int player) {
        List<TurnMove> turns = new ArrayList<>();

        // checks every square for the player's pieces
        for (int start = 0; start < board.getCellCount(); start++) {

            if (!board.isPlayableIndex(start)) {
                continue;
            }

            Piece piece = board.getPiece(start);

            if (!belongsTo(piece, player)) {
                continue;
            }

            int row = board.rowOf(start);
            int col = board.colOf(start);

            // checks each diagonal direction for a legal move
            for (int[] d : DIAGONALS) {

                // regular pieces can only move forward
                // kings can move in all four directions
                if (!piece.isKing() && !isForward(piece, d[0])) {
                    continue;
                }

                int nextRow = row + d[0];
                int nextCol = col + d[1];

                // kings can move any number of empty squares diagonally
                // regular pieces can only move one square
                while (board.isValidCoordinate(nextRow, nextCol)) {

                    int end = board.indexAt(nextRow, nextCol);

                    // stop if the square is not playable or is occupied
                    if (!board.isPlayableIndex(end) || !board.isEmpty(end)) {
                        break;
                    }

                    // add the possible move to the list
                    turns.add(new TurnMove(List.of(
                            new MoveStep(start, end, -1)
                    )));

                    // regular pieces cannot move farther than one square
                    if (!piece.isKing()) {
                        break;
                    }

                    // continue searching farther along the diagonal for kings
                    nextRow += d[0];
                    nextCol += d[1];
                }
            }
        }

        return turns;
    }

    // applies one step of a previously validated thai checkers turn
     @Override
    public void applyStep(Board board, MoveStep step, int player, boolean finalStep) {

        Piece piece = board.getPiece(step.startIndex());

        // makes sure the starting square contains the current player's piece
        if (piece == null || piece.isEmpty() || !belongsTo(piece, player)) {
            throw new IllegalArgumentException(
                    "No current player's piece at move start"
            );
        }

        // makes sure the destination square is empty
        if (!board.isEmpty(step.endIndex())) {
            throw new IllegalArgumentException("Landing square is not empty");
        }

        // moves the piece to its new location
        board.setPiece(step.endIndex(), piece);
        board.removePiece(step.startIndex());

        // removes captured pieces immediately, as required by Thai rules
        if (step.isCapture()) {
            board.removePiece(step.capturedIndex());
        }

        // checks for promotion after the final step of the turn
        promoteIfNeeded(board, step.endIndex(), player, finalStep);
    }

    // no additional capture removal is needed at the end of a turn
    // because thai rules remove captured pieces immediately
    @Override
    public void finishTurn(Board board, List<Integer> capturedIndices, int player) {
        // intentionally left empty.
    }

    // promotes a regular piece when it reaches the opponent's back row
    @Override
    public void promoteIfNeeded(Board board, int index, int player, boolean finalStep) {

        if (!finalStep) {
            return;
        }

        Piece piece = board.getPiece(index);

        if (piece == null || piece.isEmpty() || piece.isKing()) {
            return;
        }

        // a red piece is promoted at the top row
        //  black piece is promoted at the bottom row
        if (isPromotionRow(board, index, piece)) {
            piece.makeKing();
        }
    }

    // checks whether the piece has reached the row where it becomes a king
    private boolean isPromotionRow(Board board, int index, Piece piece) {
        int row = board.rowOf(index);

        return (piece.isRed() && row == 0)
                || (piece.isBlack() && row == board.getBoardSize() - 1);
    }

    // validates that the board belongs to this variant and the player is valid
    private void validateBoardAndPlayer(Board board, int player) {
        if (board == null || !getVariantId().equals(board.getConfig().getVariantId())) {
            throw new IllegalArgumentException("Wrong board");
        }

        if (player != Piece.red && player != Piece.black) {
            throw new IllegalArgumentException("Unknown player: " + player);
        }
    }

    // determines whether a regular piece is moving in its forward direction
    // red pieces move toward row 0, while black pieces move toward the last row
    private boolean isForward(Piece piece, int rowDelta) {
        return piece.isRed() ? rowDelta < 0 : rowDelta > 0;
    }

    // checks whether a piece belongs to the player whose turn it is
    private boolean belongsTo(Piece piece, int player) {
        return piece != null && !piece.isEmpty()
                && (player == Piece.red ? piece.isRed() : piece.isBlack());
    }

    // checks whether a piece belongs to the opposing player
    private boolean isOpponent(Piece target, int player) {
        return target != null && !target.isEmpty()
                && (player == Piece.red ? target.isBlack() : target.isRed());
    }

    // stores the location of the captured piece and the landing square
    private record CaptureOption(int capturedIndex, int landingIndex) {
    }

    // thai Checkers does not require maximum captures
    @Override
    public boolean requiresMaximumCapture() {
        return false;
    }

    // thai Checkers does not use the Malaysian forfeiture rule
    @Override
    public boolean forfeitsPieceWhenCaptureDeclined() {
        return false;
    }

    // Uses the default draw-turn limit from GameRules.
}
        
     

