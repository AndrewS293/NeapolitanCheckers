package com.checkers.config;

/**
 * Immutable, UI-independent configuration for a checkers board.
 * Coordinates are zero-based. Engine indices are row-major across every cell.
 */
public final class GameConfig {

    public enum SquareLayout {
        /** Only one color of squares may contain pieces (American/Malaysian). */
        DARK_SQUARES_ONLY,
        /** Every square may contain a piece (Turkish). */
        ALL_SQUARES
    }

    private final String variantId;
    private final int boardSize;
    private final int startingRowsPerPlayer;
    private final int piecesPerPlayer;
    private final SquareLayout squareLayout;
    private final boolean bottomLeftDark;

    public GameConfig(
            String variantId,
            int boardSize,
            int startingRowsPerPlayer,
            int piecesPerPlayer,
            SquareLayout squareLayout,
            boolean bottomLeftDark) {

        if (variantId == null || variantId.isBlank()) {
            throw new IllegalArgumentException("Variant ID cannot be blank");
        }
        if (boardSize < 4 || startingRowsPerPlayer < 1
                || startingRowsPerPlayer * 2 >= boardSize) {
            throw new IllegalArgumentException("Invalid board dimensions or starting rows");
        }
        if (piecesPerPlayer < 1 || squareLayout == null) {
            throw new IllegalArgumentException("Invalid board configuration");
        }

        int playableSquaresPerRow = squareLayout == SquareLayout.ALL_SQUARES
                ? boardSize
                : boardSize / 2;
        if (piecesPerPlayer > playableSquaresPerRow * startingRowsPerPlayer) {
            throw new IllegalArgumentException(
                    "Too many starting pieces for the configured starting rows");
        }

        this.variantId = variantId;
        this.boardSize = boardSize;
        this.startingRowsPerPlayer = startingRowsPerPlayer;
        this.piecesPerPlayer = piecesPerPlayer;
        this.squareLayout = squareLayout;
        this.bottomLeftDark = bottomLeftDark;
    }

    public String getVariantId() { return variantId; }
    public int getBoardSize() { return boardSize; }
    public int getCellCount() { return boardSize * boardSize; }
    public int getStartingRowsPerPlayer() { return startingRowsPerPlayer; }
    public int getPiecesPerPlayer() { return piecesPerPlayer; }
    public SquareLayout getSquareLayout() { return squareLayout; }
    public boolean isBottomLeftDark() { return bottomLeftDark; }

    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }

    public boolean isPlayableSquare(int row, int col) {
        if (!isValidCoordinate(row, col)) {
            return false;
        }
        if (squareLayout == SquareLayout.ALL_SQUARES) {
            return true;
        }

        // The bottom-left cell is (boardSize - 1, 0). Its parity defines
        // which square color is playable, including on odd-sized boards.
        int bottomLeftParity = (boardSize - 1) % 2;
        int playableParity = bottomLeftDark
                ? bottomLeftParity
                : 1 - bottomLeftParity;
        return (row + col) % 2 == playableParity;
    }

    /** Converts coordinates to a row-major engine index, or -1 if out of bounds. */
    public int toIndex(int row, int col) {
        return isValidCoordinate(row, col) ? row * boardSize + col : -1;
    }

    public int getRow(int index) {
        checkIndex(index);
        return index / boardSize;
    }

    public int getCol(int index) {
        checkIndex(index);
        return index % boardSize;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= getCellCount()) {
            throw new IndexOutOfBoundsException("Invalid board index: " + index);
        }
    }
}
