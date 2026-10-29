package com.checkers.variants.malaysian;

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

/**
 * Malaysian/Singaporean draughts (Dam/Dum), using the common 12x12 ruleset:
 * forward-only men, flying kings, mandatory maximum capture sequences, and
 * deferred capture removal. A quiet move when a capture is available is allowed
 * as a huffing action and forfeits one piece that had a capture available.
 *
 * Regional house rules exist; this implementation does not include the Penang
 * king-capture tie-break variant.
 */
public final class MalaysianRules implements GameRules {
    private static final int[][] DIAGONALS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private final GameConfig config = new GameConfig(
            "MALAYSIAN", 12, 5, 30,
            GameConfig.SquareLayout.DARK_SQUARES_ONLY, true);

    @Override public String getVariantId() { return "MALAYSIAN"; }
    @Override public String getDisplayName() { return "Malaysian Checkers"; }
    @Override public GameConfig getConfig() { return config; }

    @Override
    public void initializeBoard(Board board) {
        if (board == null || !getVariantId().equals(board.getConfig().getVariantId())) {
            throw new IllegalArgumentException("MalaysianRules requires a MALAYSIAN board");
        }
        board.reset();
    }

    @Override
    public List<TurnMove> getLegalTurns(Board board, int player) {
        validateBoardAndPlayer(board, player);
        List<TurnMove> captures = generateAllCaptures(board, player);
        if (!captures.isEmpty()) {
            int maxCaptured = captures.stream()
                    .mapToInt(turn -> (int) turn.getCapturedCount())
                    .max().orElse(0);
            List<TurnMove> maximumCaptures = captures.stream()
                    .filter(turn -> turn.getCapturedCount() == maxCaptured)
                    .toList();

            // Malaysian huffing: a player may decline a capture by making a quiet
            // move, but must then forfeit a piece that had a capture available.
            List<TurnMove> result = new ArrayList<>(maximumCaptures);
            result.addAll(generateQuietTurns(board, player));
            return List.copyOf(result);
        }
        return generateQuietTurns(board, player);
    }

    /** Returns the first capture-capable piece in deterministic board-index order. */
    @Override
    public int getPieceToForfeitWhenCaptureDeclined(Board board, int player) {
        return generateAllCaptures(board, player).stream()
                .map(TurnMove::getStartIndex)
                .min(Integer::compareTo)
                .orElse(-1);
    }

    @Override
    public int getPieceToForfeitWhenCaptureDeclined(Board board, int player, int attemptedStart) {
        boolean attemptedPieceCanCapture = generateAllCaptures(board, player).stream()
                .anyMatch(turn -> turn.getStartIndex() == attemptedStart);
        return attemptedPieceCanCapture
                ? attemptedStart
                : getPieceToForfeitWhenCaptureDeclined(board, player);
    }

    private List<TurnMove> generateAllCaptures(Board board, int player) {
        List<TurnMove> captures = new ArrayList<>();
        for (int start = 0; start < board.getCellCount(); start++) {
            if (!board.isPlayableIndex(start) || !belongsTo(board.getPiece(start), player)) continue;
            findCaptures(board, start, player, new ArrayList<>(), new HashSet<>(), captures);
        }
        return captures;
    }

    private void findCaptures(
            Board position,
            int current,
            int player,
            List<MoveStep> path,
            Set<Integer> capturedIndices,
            List<TurnMove> output) {

        Piece movingPiece = position.getPiece(current);
        if (movingPiece == null || movingPiece.isEmpty()) return;

        List<CaptureOption> options = getCaptureOptions(
                position, current, movingPiece, player, capturedIndices);

        if (options.isEmpty()) {
            if (!path.isEmpty()) output.add(new TurnMove(path));
            return;
        }

        for (CaptureOption option : options) {
            Board next = position.copy();
            Piece nextPiece = next.getPiece(current);
            next.setPiece(option.landingIndex(), nextPiece);
            next.removePiece(current);
            // Captured pieces intentionally remain on the board until the turn ends.
            List<MoveStep> nextPath = new ArrayList<>(path);
            nextPath.add(new MoveStep(current, option.landingIndex(), option.capturedIndex()));
            Set<Integer> nextCaptured = new HashSet<>(capturedIndices);
            nextCaptured.add(option.capturedIndex());
            findCaptures(next, option.landingIndex(), player, nextPath, nextCaptured, output);
        }
    }

    private List<CaptureOption> getCaptureOptions(
            Board board,
            int current,
            Piece piece,
            int player,
            Set<Integer> capturedIndices) {
        List<CaptureOption> options = new ArrayList<>();
        int row = board.rowOf(current);
        int col = board.colOf(current);

        if (!piece.isKing()) {
            for (int[] d : DIAGONALS) {
                if (!isForward(piece, d[0])) continue;
                int middle = board.indexAt(row + d[0], col + d[1]);
                int landing = board.indexAt(row + 2 * d[0], col + 2 * d[1]);
                if (middle < 0 || landing < 0
                        || !board.isPlayableIndex(middle)
                        || !board.isPlayableIndex(landing)
                        || !board.isEmpty(landing)
                        || capturedIndices.contains(middle)) continue;
                if (isOpponent(board.getPiece(middle), player)) {
                    options.add(new CaptureOption(middle, landing));
                }
            }
            return options;
        }

        // Flying king: locate the first occupied square on each diagonal. If it
        // is an uncaptured enemy, every empty landing square beyond it is legal
        // until the next occupied square.
        for (int[] d : DIAGONALS) {
            int scanRow = row + d[0];
            int scanCol = col + d[1];
            int enemyIndex = -1;
            while (board.isValidCoordinate(scanRow, scanCol)) {
                int index = board.indexAt(scanRow, scanCol);
                if (!board.isPlayableIndex(index)) break;
                Piece occupant = board.getPiece(index);
                if (occupant != null && !occupant.isEmpty()) {
                    if (enemyIndex >= 0) break;
                    if (capturedIndices.contains(index) || !isOpponent(occupant, player)) break;
                    enemyIndex = index;
                } else if (enemyIndex >= 0) {
                    options.add(new CaptureOption(enemyIndex, index));
                }
                scanRow += d[0];
                scanCol += d[1];
            }
        }
        return options;
    }

    private List<TurnMove> generateQuietTurns(Board board, int player) {
        List<TurnMove> turns = new ArrayList<>();
        for (int start = 0; start < board.getCellCount(); start++) {
            if (!board.isPlayableIndex(start)) continue;
            Piece piece = board.getPiece(start);
            if (!belongsTo(piece, player)) continue;
            int row = board.rowOf(start);
            int col = board.colOf(start);
            for (int[] d : DIAGONALS) {
                if (!piece.isKing() && !isForward(piece, d[0])) continue;
                int nextRow = row + d[0];
                int nextCol = col + d[1];
                while (board.isValidCoordinate(nextRow, nextCol)) {
                    int end = board.indexAt(nextRow, nextCol);
                    if (!board.isPlayableIndex(end) || !board.isEmpty(end)) break;
                    turns.add(new TurnMove(List.of(new MoveStep(start, end, -1))));
                    if (!piece.isKing()) break;
                    nextRow += d[0];
                    nextCol += d[1];
                }
            }
        }
        return turns;
    }

    @Override
    public void applyStep(Board board, MoveStep step, int player, boolean finalStep) {
        Piece piece = board.getPiece(step.startIndex());
        if (piece == null || piece.isEmpty() || !belongsTo(piece, player)) {
            throw new IllegalArgumentException("No current player's piece at move start");
        }
        if (!board.isEmpty(step.endIndex())) {
            throw new IllegalArgumentException("Landing square is not empty");
        }
        board.setPiece(step.endIndex(), piece);
        board.removePiece(step.startIndex());
        // Do not remove captured pieces here; Malaysian rules defer removal.
        if (finalStep) promoteIfNeeded(board, step.endIndex(), player, true);
    }

    @Override
    public void finishTurn(Board board, List<Integer> capturedIndices, int player) {
        for (int capturedIndex : capturedIndices) board.removePiece(capturedIndex);
    }

    @Override
    public void promoteIfNeeded(Board board, int index, int player, boolean finalStep) {
        if (!finalStep) return;
        Piece piece = board.getPiece(index);
        if (piece == null || piece.isEmpty() || piece.isKing()) return;
        int row = board.rowOf(index);
        if ((piece.isRed() && row == 0)
                || (piece.isBlack() && row == board.getBoardSize() - 1)) {
            piece.makeKing();
        }
    }

    @Override public boolean requiresMaximumCapture() { return true; }
    @Override public boolean forfeitsPieceWhenCaptureDeclined() { return true; }
    @Override public int getDrawTurnLimit() { return -1; }

    private void validateBoardAndPlayer(Board board, int player) {
        if (board == null || !getVariantId().equals(board.getConfig().getVariantId())) {
            throw new IllegalArgumentException("MalaysianRules requires a MALAYSIAN board");
        }
        if (player != Piece.red && player != Piece.black) {
            throw new IllegalArgumentException("Unknown player: " + player);
        }
    }

    private boolean isForward(Piece piece, int rowDelta) {
        return piece.isRed() ? rowDelta < 0 : rowDelta > 0;
    }

    private boolean belongsTo(Piece piece, int player) {
        return piece != null && !piece.isEmpty()
                && (player == Piece.red ? piece.isRed() : piece.isBlack());
    }

    private boolean isOpponent(Piece target, int player) {
        return target != null && !target.isEmpty()
                && (player == Piece.red ? target.isBlack() : target.isRed());
    }

    private record CaptureOption(int capturedIndex, int landingIndex) { }
}
