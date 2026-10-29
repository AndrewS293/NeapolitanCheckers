package com.checkers.variants.american;

import com.checkers.config.GameConfig;
import com.checkers.logic.Board;
import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Move.TurnMove;
import com.checkers.logic.Piece;
import com.checkers.rules.GameRules;

import java.util.ArrayList;
import java.util.List;

/** Standard American checkers rules, using configurable row-major board indices. */
public final class AmericanRules implements GameRules {
    private static final int[][] DIRECTIONS = {{-1,-1},{-1,1},{1,-1},{1,1}};
    private final GameConfig config;

    public AmericanRules() {
        this(new GameConfig("AMERICAN", 8, 3, 12,
                GameConfig.SquareLayout.DARK_SQUARES_ONLY, true));
    }

    public AmericanRules(GameConfig config) {
        if (config == null || !"AMERICAN".equals(config.getVariantId())) {
            throw new IllegalArgumentException("AmericanRules requires an AMERICAN GameConfig");
        }
        this.config = config;
    }

    @Override public String getVariantId() { return "AMERICAN"; }
    @Override public String getDisplayName() { return "American Checkers"; }
    @Override public GameConfig getConfig() { return config; }

    @Override public List<TurnMove> getLegalTurns(Board board, int player) {
        List<TurnMove> captures = new ArrayList<>();
        for (int start = 0; start < board.getCellCount(); start++) {
            Piece piece = board.getPiece(start);
            if (board.isPlayableIndex(start) && belongsTo(piece, player)) {
                findCaptures(board, start, player, new ArrayList<>(), captures);
            }
        }
        if (!captures.isEmpty()) return List.copyOf(captures);

        List<TurnMove> moves = new ArrayList<>();
        for (int start = 0; start < board.getCellCount(); start++) {
            Piece piece = board.getPiece(start);
            if (!board.isPlayableIndex(start) || !belongsTo(piece, player)) continue;
            int row = board.rowOf(start), col = board.colOf(start);
            for (int[] direction : DIRECTIONS) {
                if (!piece.isKing() && !forwardDirection(piece, direction[0])) continue;
                int end = board.indexAt(row + direction[0], col + direction[1]);
                if (end >= 0 && board.isPlayableIndex(end) && board.isEmpty(end)) {
                    moves.add(new TurnMove(List.of(new MoveStep(start, end, -1))));
                }
            }
        }
        return List.copyOf(moves);
    }

    private void findCaptures(Board board, int current, int player,
                              List<MoveStep> path, List<TurnMove> output) {
        Piece piece = board.getPiece(current);
        if (piece == null || piece.isEmpty()) return;
        boolean found = false;
        int row = board.rowOf(current), col = board.colOf(current);
        for (int[] direction : DIRECTIONS) {
            if (!piece.isKing() && !forwardDirection(piece, direction[0] * 2)) continue;
            int middle = board.indexAt(row + direction[0], col + direction[1]);
            int end = board.indexAt(row + 2 * direction[0], col + 2 * direction[1]);
            if (middle < 0 || end < 0 || !board.isPlayableIndex(middle)
                    || !board.isPlayableIndex(end) || !board.isEmpty(end)
                    || !isOpponent(board.getPiece(current), board.getPiece(middle), player)) continue;

            found = true;
            Piece capturedPiece = board.getPiece(middle);
            int originalType = piece.getType();
            board.setPiece(end, piece);
            board.removePiece(current);
            board.removePiece(middle);
            boolean promoted = promote(board, end);
            path.add(new MoveStep(current, end, middle));

            // American rules end the capture turn when a man is crowned.
            if (promoted) {
                output.add(new TurnMove(path));
            } else {
                int before = output.size();
                findCaptures(board, end, player, path, output);
                if (output.size() == before) output.add(new TurnMove(path));
            }

            path.remove(path.size() - 1);
            piece.setType(originalType);
            board.setPiece(current, piece);
            board.setPiece(middle, capturedPiece);
            board.removePiece(end);
        }
        if (!found && !path.isEmpty()) {
            // A leaf is normally recorded by its parent; this protects direct leaf calls.
        }
    }

    private boolean promote(Board board, int index) {
        Piece piece = board.getPiece(index);
        if (piece == null || piece.isEmpty() || piece.isKing()) return false;
        int row = board.rowOf(index);
        if ((piece.isRed() && row == 0) || (piece.isBlack() && row == board.getBoardSize() - 1)) {
            piece.makeKing();
            return true;
        }
        return false;
    }

    private boolean forwardDirection(Piece piece, int rowDelta) {
        return piece.isRed() ? rowDelta < 0 : rowDelta > 0;
    }

    private boolean belongsTo(Piece piece, int player) {
        return piece != null && !piece.isEmpty()
                && (player == Piece.red ? piece.isRed() : piece.isBlack());
    }

    private boolean isOpponent(Piece mover, Piece target, int player) {
        return target != null && !target.isEmpty() && belongsTo(mover, player)
                && !belongsTo(target, player);
    }

    @Override public boolean requiresMaximumCapture() { return false; }
    @Override public boolean forfeitsPieceWhenCaptureDeclined() { return false; }
}
