
package com.checkers.variants.turkish;

import com.checkers.config.GameConfig;
import com.checkers.logic.Board;
import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Move.TurnMove;
import com.checkers.logic.Piece;
import com.checkers.rules.GameRules;

import java.util.ArrayList;
import java.util.List;

public final class TurkishRules implements GameRules {
    private static final int[][] DIRECTIONS = {{-1,0},{1,0},{0,-1},{0,1}};
    private final GameConfig config;

    public TurkishRules() {
      this(new GameConfig("TURKISH", 8, 2, 16,
                GameConfig.SquareLayout.ALL_SQUARES, true));
    }

    public TurkishRules(GameConfig config) {
        if (config == null || !"TURKISH".equals(config.getVariantId())) {
            throw new IllegalArgumentException("Wrong GameConfig");
        }
        this.config = config;
    }

    @Override public String getVariantId() {
        return "TURKISH";
    }
    @Override public String getDisplayName() {
        return "Turkish Checkers";
    }
    @Override public GameConfig getConfig() {
        return config;
    }@Override
public void initializeBoard(Board board) {
    board.clear();

    for (int col = 0; col < 8; col++) {
        board.setPiece(board.indexAt(1, col), new Piece(Piece.black));
        board.setPiece(board.indexAt(2, col), new Piece(Piece.black));

        board.setPiece(board.indexAt(5, col), new Piece(Piece.red));
        board.setPiece(board.indexAt(6, col), new Piece(Piece.red));
    }
}

   //get legal moves of each piece
    @Override public List<TurnMove> getLegalTurns(Board board, int player) {
        List<TurnMove> captures = new ArrayList<>();
        for (int start = 0; start < board.getCellCount(); start++) {
            Piece piece = board.getPiece(start);
            if (board.isPlayableIndex(start) && belongsTo(piece, player)) {
                findCaptures(board, start, player, new ArrayList<>(), captures);
            }
        }
        if (!captures.isEmpty()) {
        int max = 0;
        for (TurnMove move : captures) {
            if (move.steps().size() > max) {
                max = move.steps().size();
          }
        }

        List<TurnMove> moves = new ArrayList<>();
        for (TurnMove move : captures) {
            if (move.steps().size() == max) {
              moves.add(move);
            }
          }
              return List.copyOf(moves);
        }

          List<TurnMove> moves = new ArrayList<>();
        for (int start = 0; start < board.getCellCount(); start++) {
            Piece piece = board.getPiece(start);
            if (!board.isPlayableIndex(start) || !belongsTo(piece, player)) continue;
            int row = board.rowOf(start), col = board.colOf(start);
            for (int[] direction : DIRECTIONS) {
                if (!piece.isKing() && direction[0] != 0 && !forwardDirection(piece, direction[0])) continue;
                int newRow = row + direction[0], newCol = col + direction[1];
                while (board.isValidCoordinate(newRow, newCol)) {
                    int end = board.indexAt(newRow, newCol);
                    if (board.isPlayableIndex(end) && board.isEmpty(end)) {
                        moves.add(new TurnMove(List.of(new MoveStep(start, end, -1))));
                    } else {
                        break;
                    }
                    if (!piece.isKing()) break;

                    newRow += direction[0];
                    newCol += direction[1];
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

        if (piece.isKing()) {
             for (int[] d : DIRECTIONS) {
                int scanRow = row + d[0];
                int scanCol = col + d[1];
                int enemyIndex = -1;
                while (board.isValidCoordinate(scanRow, scanCol)) {
                    int index = board.indexAt(scanRow, scanCol);
                    if (!board.isPlayableIndex(index)) break;
                    Piece occupant = board.getPiece(index);
                    if (occupant != null && !occupant.isEmpty()) {
                        if (enemyIndex >= 0) break;
                        if (!isOpponent(piece, occupant, player)) break;
                        enemyIndex = index;
                    } else if (enemyIndex >= 0) {
                        Piece capturedPiece = board.getPiece(enemyIndex);
                        board.removePiece(enemyIndex);
                        board.setPiece(index, piece);
                        board.removePiece(current);
                        path.add(new MoveStep(current, index, enemyIndex));
                        int before = output.size();

                        findCaptures(board, index, player, path, output);
                        if (output.size() == before) {
                            output.add(new TurnMove(path));
                        }

                        path.remove(path.size() - 1);
                        board.setPiece(current, piece);
                        board.setPiece(enemyIndex, capturedPiece);
                        board.removePiece(index);
                    }

                    scanRow += d[0];
                    scanCol += d[1];
                }
            }
            return;
        }

        for (int[] direction : DIRECTIONS) {
            int middle = board.indexAt(row + direction[0], col + direction[1]);
            int end = board.indexAt(row + 2 * direction[0], col + 2 * direction[1]);
            if (middle < 0 || end < 0 || !board.isPlayableIndex(middle)
                    || !board.isPlayableIndex(end) || !board.isEmpty(end)
                    || !isOpponent(board.getPiece(current), board.getPiece(middle), player)) continue;

        
            Piece capturedPiece = board.getPiece(middle);
            int originalType = piece.getType();
            board.setPiece(end, piece);
            board.removePiece(current);
            board.removePiece(middle);
            path.add(new MoveStep(current, end, middle));
            int before = output.size();
                findCaptures(board, end, player, path, output);
                if (output.size() == before) 
                  output.add(new TurnMove(path));
                
            

            path.remove(path.size() - 1);
            piece.setType(originalType);
            board.setPiece(current, piece);
            board.setPiece(middle, capturedPiece);
            board.removePiece(end);
        }
        
    }

    //make king if at final row
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

    @Override public boolean requiresMaximumCapture() {
        return true;
    }
    @Override public boolean forfeitsPieceWhenCaptureDeclined() {
        return false;
    }

    
}