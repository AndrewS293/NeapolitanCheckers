package com.checkers.rules;

import com.checkers.config.GameConfig;
import com.checkers.logic.Board;
import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Move.TurnMove;

import java.util.List;

/** Variant policy contract. Implementations must be stateless between games. */
public interface GameRules {
    String getVariantId();
    String getDisplayName();
    GameConfig getConfig();

    /** Reset the board to this variant's initial position. */
    default void initializeBoard(Board board) { board.reset(); }

    /** Generate complete legal turns, not merely individual steps. */
    List<TurnMove> getLegalTurns(Board board, int player);

    default boolean isLegalTurn(Board board, TurnMove turn, int player) {
        return getLegalTurns(board, player).contains(turn);
    }

    /** Apply a complete, previously validated turn. */
    default void applyTurn(Board board, TurnMove turn, int player) {
        List<Integer> captured = new java.util.ArrayList<>();
        for (int i = 0; i < turn.steps().size(); i++) {
            MoveStep step = turn.steps().get(i);
            applyStep(board, step, player, i == turn.steps().size() - 1);
            if (step.isCapture()) captured.add(step.capturedIndex());
        }
        finishTurn(board, captured, player);
    }

    /** Hook for stepwise clients; override when a variant defers capture removal. */
    default void applyStep(Board board, MoveStep step, int player, boolean finalStep) {
        var piece = board.getPiece(step.startIndex());
        if (piece == null || piece.isEmpty()) throw new IllegalArgumentException("No piece at move start");
        board.setPiece(step.endIndex(), piece);
        board.removePiece(step.startIndex());
        if (step.isCapture()) board.removePiece(step.capturedIndex());
        promoteIfNeeded(board, step.endIndex(), player, finalStep);
    }

    /** Called at the end of a turn; variants may remove deferred captures here. */
    default void finishTurn(Board board, List<Integer> capturedIndices, int player) { }

    /** Promote only when this variant's timing rules permit it. */
    default void promoteIfNeeded(Board board, int index, int player, boolean finalStep) {
        var piece = board.getPiece(index);
        if (piece == null || piece.isEmpty()) return;
        int row = board.rowOf(index);
        if ((piece.isRed() && row == 0)
                || (piece.isBlack() && row == board.getBoardSize() - 1)) {
            piece.makeKing();
        }
    }

    default boolean forfeitsPieceWhenCaptureDeclined() { return false; }

    /**
     * Selects a piece to forfeit when a player makes a quiet move despite having
     * a capture. Return -1 when no forfeiture applies. The returned index is its
     * location before the attempted turn.
     */
    default int getPieceToForfeitWhenCaptureDeclined(Board board, int player) { return -1; }

    /** Prefer the attempted piece when it was itself able to capture. */
    default int getPieceToForfeitWhenCaptureDeclined(Board board, int player, int attemptedStart) {
        return getPieceToForfeitWhenCaptureDeclined(board, player);
    }
    default boolean requiresMaximumCapture() { return false; }
    /** Positive value enables a no-progress draw limit; non-positive disables it. */
    default int getDrawTurnLimit() { return 80; }
}
