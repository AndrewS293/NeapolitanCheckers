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


public final class MalaysianRules implements GameRules {
    private static final int[][] DIAGONALS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private final GameConfig config = new GameConfig(
            "MALAYSIAN", 12, 5, 30,
            GameConfig.SquareLayout.DARK_SQUARES_ONLY, true);

    @Override public String getVariantId() { 
        return "MALAYSIAN"; 
    }
    @Override public String getDisplayName() { 
        return "Malaysian Checkers"; 
    }
    @Override public GameConfig getConfig() { 
        return config; 
    }

    @Override
    public void initializeBoard(Board board) {
        if (board == null || !getVariantId().equals(board.getConfig().getVariantId())) {
            throw new IllegalArgumentException("Wrong board");
        }
        board.reset();
    }

    @Override
    public List<TurnMove> getLegalTurns(Board board, int player) {
        //checks for invalid board or player
        validateBoardAndPlayer(board, player);
        List<TurnMove> captures = generateAllCaptures(board, player);
        //if capture possible - check for max
        if (!captures.isEmpty()) {
            int maxCaptured = captures.stream()
                    .mapToInt(turn -> (int) turn.getCapturedCount())
                    .max().orElse(0);
            List<TurnMove> maximumCaptures = captures.stream()
                    .filter(turn -> turn.getCapturedCount() == maxCaptured)
                    .toList();

           
            List<TurnMove> result = new ArrayList<>(maximumCaptures);
            //if move != max then forfeit piece
            result.addAll(generateQuietTurns(board, player));
            return List.copyOf(result);
        }
        return generateQuietTurns(board, player);
    }

    
    //finds which piece to remove when forfeiting - by board order
    @Override
    public int getPieceToForfeitWhenCaptureDeclined(Board board, int player) {
        return generateAllCaptures(board, player).stream()
                .map(TurnMove::getStartIndex)
                .min(Integer::compareTo)
                .orElse(-1);
    }

    @Override
    //finds which piece to remove when forfeiting - by first choice selected
    public int getPieceToForfeitWhenCaptureDeclined(Board board, int player, int attemptedStart) {
        boolean attemptedPieceCanCapture = generateAllCaptures(board, player).stream()
                .anyMatch(turn -> turn.getStartIndex() == attemptedStart);
        return attemptedPieceCanCapture
                ? attemptedStart
                : getPieceToForfeitWhenCaptureDeclined(board, player);
    }

    //goes through board to find any possible capture
    private List<TurnMove> generateAllCaptures(Board board, int player) {
        List<TurnMove> captures = new ArrayList<>();
        for (int start = 0; start < board.getCellCount(); start++) {
            if (!board.isPlayableIndex(start) || !belongsTo(board.getPiece(start), player)) continue;
            findCaptures(board, start, player, new ArrayList<>(), new HashSet<>(), captures);
        }
        return captures;
    }

    //determines if a move would be a capture
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

        //recursion to find all captures of a piece
        for (CaptureOption option : options) {
            Board next = position.copy();
            Piece nextPiece = next.getPiece(current);
            next.setPiece(option.landingIndex(), nextPiece);
            next.removePiece(current);
            //pieces don't disappear until the end of the turn
            List<MoveStep> nextPath = new ArrayList<>(path);
            nextPath.add(new MoveStep(current, option.landingIndex(), option.capturedIndex()));
            Set<Integer> nextCaptured = new HashSet<>(capturedIndices);
            nextCaptured.add(option.capturedIndex());
            findCaptures(next, option.landingIndex(), player, nextPath, nextCaptured, output);
        }
    }

    //checks possible capture 
    private List<CaptureOption> getCaptureOptions(
            Board board,
            int current,
            Piece piece,
            int player,
            Set<Integer> capturedIndices) {
        List<CaptureOption> options = new ArrayList<>();
        int row = board.rowOf(current);
        int col = board.colOf(current);

        //if not king, just checks each forward move
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

        // if king - malaysian rules has flying kings, can go as far as wants diagonally and jump if empty cell behind
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

    //generates possible non-capture moves - if there is a capture available - this will lead to a forfeited piece
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
    //applies a move, allows for multicapture
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
        // wait on removing piece
        if (finalStep) promoteIfNeeded(board, step.endIndex(), player, true);
    }

    @Override
    //turn end - pieces are now removed
    public void finishTurn(Board board, List<Integer> capturedIndices, int player) {
        for (int capturedIndex : capturedIndices) board.removePiece(capturedIndex);
    }

    @Override
    //turn finished, king is now promoted
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

    @Override public boolean requiresMaximumCapture() { 
        return true; 
    }
    @Override public boolean forfeitsPieceWhenCaptureDeclined() { 
        return true; 
    }
    @Override public int getDrawTurnLimit() {
        return -1; 
    }

    private void validateBoardAndPlayer(Board board, int player) {
        if (board == null || !getVariantId().equals(board.getConfig().getVariantId())) {
            throw new IllegalArgumentException("Wrong Board");
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
