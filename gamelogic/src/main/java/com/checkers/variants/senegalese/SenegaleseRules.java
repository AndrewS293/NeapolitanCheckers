package com.checkers.variants.senegalese;

import com.checkers.config.GameConfig;
import com.checkers.logic.Board;
import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Move.TurnMove;
import com.checkers.logic.Piece;
import com.checkers.rules.GameRules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Rules for the Senegalese Checkers variant.
 *
 * This version uses the 10 x 10 international draughts rules
 * commonly played in Senegal.
 */
public final class SenegaleseRules implements GameRules {

    // All four diagonal directions:
    // up-left, up-right, down-left, and down-right.
    private static final int[][] DIAGONALS = {
            { -1, -1 },
            { -1, 1 },
            { 1, -1 },
            { 1, 1 }
    };

    /*
     * Senegalese Checkers setup:
     * - 10 x 10 board
     * - 4 starting rows per player
     * - 20 pieces per player
     * - Only dark squares are playable
     */
    private final GameConfig config = new GameConfig(
            "SENEGALESE",
            10,
            4,
            20,
            GameConfig.SquareLayout.DARK_SQUARES_ONLY,
            true);

    // Unique name used by the game engine.
    @Override
    public String getVariantId() {
        return "SENEGALESE";
    }

    // Name displayed in the GUI.
    @Override
    public String getDisplayName() {
        return "Senegalese Checkers";
    }

    // Returns the board configuration for this variant.
    @Override
    public GameConfig getConfig() {
        return config;
    }

    // Places all pieces in their starting positions.
    @Override
    public void initializeBoard(Board board) {
        validateBoardAndPlayer(board, Piece.red);
        board.reset();
    }

    /*
     * Returns every legal turn for the selected player.
     *
     * Captures are mandatory. If multiple captures are possible,
     * the player must choose a sequence that captures the most pieces.
     */
    @Override
    public List<TurnMove> getLegalTurns(Board board, int player) {
        validateBoardAndPlayer(board, player);

        List<TurnMove> captures = generateAllCaptures(board, player);

        if (!captures.isEmpty()) {
            long maximumCaptured = captures.stream()
                    .mapToLong(TurnMove::getCapturedCount)
                    .max()
                    .orElse(0);

            return captures.stream()
                    .filter(turn -> turn.getCapturedCount() == maximumCaptured)
                    .toList();
        }

        // Normal moves are allowed only when no capture exists.
        return generateQuietTurns(board, player);
    }

    // Searches the board for every possible capture sequence.
    private List<TurnMove> generateAllCaptures(
            Board board,
            int player) {

        List<TurnMove> captures = new ArrayList<>();

        for (int start = 0; start < board.getCellCount(); start++) {

            if (!board.isPlayableIndex(start)) {
                continue;
            }

            if (!belongsTo(board.getPiece(start), player)) {
                continue;
            }

            findCaptures(
                    board,
                    start,
                    player,
                    new ArrayList<>(),
                    new HashSet<>(),
                    captures);
        }

        return captures;
    }

    /*
     * Recursively searches for multijumps.
     *
     * The same piece must continue jumping until no additional
     * capture is available.
     */
    private void findCaptures(
            Board position,
            int current,
            int player,
            List<MoveStep> path,
            Set<Integer> capturedIndices,
            List<TurnMove> output) {

        Piece movingPiece = position.getPiece(current);

        if (movingPiece == null || movingPiece.isEmpty()) {
            return;
        }

        List<CaptureOption> options = getCaptureOptions(
                position,
                current,
                movingPiece,
                player,
                capturedIndices);

        // No more captures means the complete sequence is saved.
        if (options.isEmpty()) {
            if (!path.isEmpty()) {
                output.add(new TurnMove(path));
            }

            return;
        }

        // Explore every available capture path.
        for (CaptureOption option : options) {
            Board next = position.copy();
            Piece nextPiece = next.getPiece(current);

            // Move the piece to its new location.
            next.setPiece(option.landingIndex(), nextPiece);
            next.removePiece(current);

            /*
             * Captured pieces remain on the board until the complete
             * multijump finishes. The set prevents the same piece
             * from being captured twice.
             */
            Set<Integer> nextCaptured = new HashSet<>(capturedIndices);

            nextCaptured.add(option.capturedIndex());

            List<MoveStep> nextPath = new ArrayList<>(path);

            nextPath.add(
                    new MoveStep(
                            current,
                            option.landingIndex(),
                            option.capturedIndex()));

            findCaptures(
                    next,
                    option.landingIndex(),
                    player,
                    nextPath,
                    nextCaptured,
                    output);
        }
    }

    /*
     * Finds all captures available from one square.
     *
     * Regular pieces can capture forward or backward.
     * Kings can capture across long diagonal distances.
     */
    private List<CaptureOption> getCaptureOptions(
            Board board,
            int current,
            Piece piece,
            int player,
            Set<Integer> capturedIndices) {

        List<CaptureOption> options = new ArrayList<>();

        int row = board.rowOf(current);
        int col = board.colOf(current);

        // Regular pieces capture in all four diagonal directions.
        if (!piece.isKing()) {
            for (int[] direction : DIAGONALS) {
                int middle = board.indexAt(
                        row + direction[0],
                        col + direction[1]);

                int landing = board.indexAt(
                        row + 2 * direction[0],
                        col + 2 * direction[1]);

                if (middle < 0 || landing < 0) {
                    continue;
                }

                if (!board.isPlayableIndex(middle)
                        || !board.isPlayableIndex(landing)) {
                    continue;
                }

                if (!board.isEmpty(landing)) {
                    continue;
                }

                if (capturedIndices.contains(middle)) {
                    continue;
                }

                if (isOpponent(
                        board.getPiece(middle),
                        player)) {

                    options.add(
                            new CaptureOption(
                                    middle,
                                    landing));
                }
            }

            return options;
        }

        /*
         * Kings are flying kings.
         *
         * They may travel across empty diagonal squares, capture
         * the first opponent found, and land on any empty square
         * beyond that opponent.
         */
        for (int[] direction : DIAGONALS) {
            int scanRow = row + direction[0];
            int scanCol = col + direction[1];

            int enemyIndex = -1;

            while (board.isValidCoordinate(
                    scanRow,
                    scanCol)) {

                int index = board.indexAt(
                        scanRow,
                        scanCol);

                if (!board.isPlayableIndex(index)) {
                    break;
                }

                Piece occupant = board.getPiece(index);

                if (occupant != null
                        && !occupant.isEmpty()) {

                    /*
                     * A king cannot jump over two occupied squares,
                     * one of its own pieces, or a piece that was
                     * already captured during this turn.
                     */
                    if (enemyIndex >= 0
                            || capturedIndices.contains(index)
                            || !isOpponent(occupant, player)) {

                        break;
                    }

                    enemyIndex = index;
                } else if (enemyIndex >= 0) {
                    /*
                     * Every empty square after the opponent is a
                     * possible landing square.
                     */
                    options.add(
                            new CaptureOption(
                                    enemyIndex,
                                    index));
                }

                scanRow += direction[0];
                scanCol += direction[1];
            }
        }

        return options;
    }

    /*
     * Generates regular non-capturing moves.
     *
     * Regular pieces move one square forward.
     * Kings may move any distance diagonally.
     */
    private List<TurnMove> generateQuietTurns(
            Board board,
            int player) {

        List<TurnMove> turns = new ArrayList<>();

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

            for (int[] direction : DIAGONALS) {
                /*
                 * Regular pieces move forward only.
                 * Kings can move in every diagonal direction.
                 */
                if (!piece.isKing()
                        && !isForward(
                                piece,
                                direction[0])) {

                    continue;
                }

                int nextRow = row + direction[0];
                int nextCol = col + direction[1];

                while (board.isValidCoordinate(
                        nextRow,
                        nextCol)) {

                    int end = board.indexAt(
                            nextRow,
                            nextCol);

                    if (!board.isPlayableIndex(end)
                            || !board.isEmpty(end)) {

                        break;
                    }

                    turns.add(
                            new TurnMove(
                                    List.of(
                                            new MoveStep(
                                                    start,
                                                    end,
                                                    -1))));

                    // A regular piece moves only one square.
                    if (!piece.isKing()) {
                        break;
                    }

                    // A king may continue farther down the diagonal.
                    nextRow += direction[0];
                    nextCol += direction[1];
                }
            }
        }

        return turns;
    }

    /*
     * Applies one step of a move.
     *
     * Captured pieces are not removed here because they must stay
     * on the board until the complete capture sequence finishes.
     */
    @Override
    public void applyStep(
            Board board,
            MoveStep step,
            int player,
            boolean finalStep) {

        Piece piece = board.getPiece(
                step.startIndex());

        if (piece == null
                || piece.isEmpty()
                || !belongsTo(piece, player)) {

            throw new IllegalArgumentException(
                    "No current player's piece at move start");
        }

        if (!board.isEmpty(step.endIndex())) {
            throw new IllegalArgumentException(
                    "Landing square is not empty");
        }

        board.setPiece(
                step.endIndex(),
                piece);

        board.removePiece(
                step.startIndex());

        /*
         * A piece becomes a king only after the full turn ends.
         * Passing through the promotion row during a multijump
         * does not immediately create a king.
         */
        if (finalStep) {
            promoteIfNeeded(
                    board,
                    step.endIndex(),
                    player,
                    true);
        }
    }

    // Removes all captured pieces after the complete turn finishes.
    @Override
    public void finishTurn(
            Board board,
            List<Integer> capturedIndices,
            int player) {

        for (int capturedIndex : capturedIndices) {
            board.removePiece(capturedIndex);
        }
    }

    // Promotes a regular piece when it ends on the final row.
    @Override
    public void promoteIfNeeded(
            Board board,
            int index,
            int player,
            boolean finalStep) {

        if (!finalStep) {
            return;
        }

        Piece piece = board.getPiece(index);

        if (piece == null
                || piece.isEmpty()
                || piece.isKing()) {

            return;
        }

        int row = board.rowOf(index);

        if ((piece.isRed() && row == 0)
                || (piece.isBlack()
                        && row == board.getBoardSize() - 1)) {

            piece.makeKing();
        }
    }

    // Confirms that this variant requires the longest capture.
    @Override
    public boolean requiresMaximumCapture() {
        return true;
    }

    // Makes sure the correct board and player were provided.
    private void validateBoardAndPlayer(
            Board board,
            int player) {

        if (board == null
                || !getVariantId().equals(
                        board.getConfig().getVariantId())) {

            throw new IllegalArgumentException(
                    "Wrong board");
        }

        if (player != Piece.red
                && player != Piece.black) {

            throw new IllegalArgumentException(
                    "Unknown player: " + player);
        }
    }

    // Determines the forward direction for a regular piece.
    private boolean isForward(
            Piece piece,
            int rowDelta) {

        return piece.isRed()
                ? rowDelta < 0
                : rowDelta > 0;
    }

    // Checks whether a piece belongs to the selected player.
    private boolean belongsTo(
            Piece piece,
            int player) {

        return piece != null
                && !piece.isEmpty()
                && (player == Piece.red
                        ? piece.isRed()
                        : piece.isBlack());
    }

    // Checks whether a piece belongs to the opponent.
    private boolean isOpponent(
            Piece piece,
            int player) {

        return piece != null
                && !piece.isEmpty()
                && (player == Piece.red
                        ? piece.isBlack()
                        : piece.isRed());
    }

    // Stores one possible captured piece and landing square.
    private record CaptureOption(
            int capturedIndex,
            int landingIndex) {
    }
}