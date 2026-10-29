
package com.checkers.logic;

import com.checkers.config.GameConfig;
import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Move.TurnMove;
import com.checkers.rules.GameRules;
import com.checkers.variants.american.AmericanRules;
import com.checkers.variants.malaysian.MalaysianRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Game state and turn management, with variant-specific behavior delegated
// to GameRules.
public class GameLogic {

    public static final int red_player = Piece.red;
    public static final int black_player = Piece.black;

    // Retained for compatibility with existing code.
    public static final int DRAW_TURN_LIMIT = 80;

    public enum GameState {
        IN_PROGRESS,
        RED_WON,
        BLACK_WON,
        DRAW
    }

    private final Board board;
    private final GameRules rules;

    private final Map<String, Integer> positionCounts = new HashMap<>();

    private int currentPlayer = red_player;
    private int forcedJumpIndex = -1;
    private int turnsWithoutProgress = 0;
    private GameState gameState = GameState.IN_PROGRESS;

    // A complete turn can consist of several steps. The JavaFX interface
    // submits one step at a time, so retain the matching complete-turn paths.
    private List<TurnMove> pendingTurns = List.of();
    private int pendingStepIndex = 0;
    private int turnStartIndex = -1;
    private int pendingForfeitIndex = -1;
    private boolean turnHadProgress = false;

    /** Creates a default American checkers game. */
    public GameLogic() {
        this(new AmericanRules());
    }

    /**
     * Backward-compatible constructor.
     * Variant-specific geometry must match an available rules implementation.
     */
    public GameLogic(GameConfig config) {
        this(rulesForConfig(config));
    }

    /** Creates a game using the supplied variant's rules. */
    public GameLogic(GameRules rules) {
        if (rules == null || rules.getConfig() == null) {
            throw new IllegalArgumentException(
                    "GameRules and its GameConfig cannot be null.");
        }

        this.rules = rules;
        this.board = new Board(rules.getConfig());
        this.rules.initializeBoard(this.board);
        recordCurrentPosition();
    }

    private static GameRules rulesForConfig(GameConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("GameConfig cannot be null.");
        }

        return switch (config.getVariantId()) {
            case "AMERICAN" -> new AmericanRules(config);
            case "MALAYSIAN" -> {
                GameRules malaysian = new MalaysianRules();
                if (!sameConfig(config, malaysian.getConfig())) {
                    throw new IllegalArgumentException(
                            "The supplied MALAYSIAN configuration does not "
                            + "match the Malaysian rules configuration.");
                }
                yield malaysian;
            }
            default -> throw new IllegalArgumentException(
                    "No rules implementation is registered for variant: "
                    + config.getVariantId());
        };
    }

    private static boolean sameConfig(GameConfig a, GameConfig b) {
        return a.getVariantId().equals(b.getVariantId())
                && a.getBoardSize() == b.getBoardSize()
                && a.getStartingRowsPerPlayer()
                    == b.getStartingRowsPerPlayer()
                && a.getPiecesPerPlayer() == b.getPiecesPerPlayer()
                && a.getSquareLayout() == b.getSquareLayout()
                && a.isBottomLeftDark() == b.isBottomLeftDark();
    }

    /** Creates an independent copy of an existing game. */
    private GameLogic(GameLogic source) {
        this.rules = source.rules;
        this.board = source.board.copy();
        this.positionCounts.putAll(source.positionCounts);
        this.currentPlayer = source.currentPlayer;
        this.forcedJumpIndex = source.forcedJumpIndex;
        this.turnsWithoutProgress = source.turnsWithoutProgress;
        this.gameState = source.gameState;
        this.pendingTurns = List.copyOf(source.pendingTurns);
        this.pendingStepIndex = source.pendingStepIndex;
        this.turnStartIndex = source.turnStartIndex;
        this.pendingForfeitIndex = source.pendingForfeitIndex;
        this.turnHadProgress = source.turnHadProgress;
    }

    /** Returns an independent snapshot of this game. */
    public GameLogic copy() {
        return new GameLogic(this);
    }

    public GameRules getRules() {
        return rules;
    }
    
    public Board getBoard() {
        return board;
    }

    public GameConfig getConfig() {
        return board.getConfig();
    }


    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isGameOver() {
        return gameState != GameState.IN_PROGRESS;
    }

    public int getForcedJumpIndex() {
        return forcedJumpIndex;
    }

    public boolean isMultiJumpInProgress() {
        return forcedJumpIndex != -1;
    }

    public int getTurnsWithoutProgress() {
        return turnsWithoutProgress;
    }

    public String getGameResultMessage() {
        return switch (gameState) {
            case RED_WON -> "Red wins!";
            case BLACK_WON -> "Black wins!";
            case DRAW -> "The game is a tie.";
            case IN_PROGRESS -> "Game in progress";
        };
    }

    /** Retained for compatibility with existing callers. */
    public void switchPlayer() {
        if (forcedJumpIndex != -1) {
            throw new IllegalStateException(
                    "Cannot switch players before the turn is complete.");
        }
        currentPlayer = otherPlayer(currentPlayer);
    }

    public boolean isCurrentPlayerPiece(int index) {
        return board.isValidIndex(index)
                && board.isPlayableIndex(index)
                && belongsToPlayer(board.getPiece(index), currentPlayer);
    }

    /**
     * Applies one UI-selected step.
     *
     * The rules implementation determines whether it is a legal step and
     * how to apply it. The turn is completed only after its full path ends.
     */
    public boolean move(int start, int end) {
        if (isGameOver()
                || !board.isValidIndex(start)
                || !board.isValidIndex(end)
                || !board.isPlayableIndex(start)
                || !board.isPlayableIndex(end)
                || !board.isEmpty(end)
                || !isCurrentPlayerPiece(start)) {
            return false;
        }

        if (forcedJumpIndex != -1 && start != forcedJumpIndex) {
            return false;
        }

        List<TurnMove> availableTurns = getTurnsForCurrentDecision();
        List<TurnMove> matchingTurns = matchingNextSteps(
                availableTurns, start, end);

        if (matchingTurns.isEmpty()) {
            return false;
        }

        MoveStep step = matchingTurns.getFirst()
                .steps().get(pendingStepIndex);

        boolean finalStep = matchingTurns.stream()
                .allMatch(turn ->
                        turn.steps().size() == pendingStepIndex + 1);

        // Determine huffing before moving anything. Once a piece moves,
        // its original square is no longer occupied.
        if (forcedJumpIndex == -1) {
            turnStartIndex = start;
            turnHadProgress = false;
            pendingForfeitIndex = -1;

            boolean captureWasAvailable = availableTurns.stream()
                    .anyMatch(turn -> turn.steps().getFirst().isCapture());

            boolean selectedCapture = step.isCapture();

            if (captureWasAvailable
                    && !selectedCapture
                    && rules.forfeitsPieceWhenCaptureDeclined()) {
                pendingForfeitIndex =
                        rules.getPieceToForfeitWhenCaptureDeclined(
                                board, currentPlayer, start);
            }
        }

        Piece movingPiece = board.getPiece(step.startIndex());
        boolean wasKing = movingPiece.isKing();

        // Variant-specific behavior:
        // American removes captured pieces immediately;
        // Malaysian defers their removal until the turn ends.
        try {
            rules.applyStep(board, step, currentPlayer, finalStep);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        Piece pieceAfterMove = board.getPiece(step.endIndex());
        boolean promoted = pieceAfterMove != null
                && !pieceAfterMove.isEmpty()
                && !wasKing
                && pieceAfterMove.isKing();

        if (step.isCapture() || promoted) {
            turnHadProgress = true;
        }

        if (!finalStep) {
            pendingTurns = List.copyOf(matchingTurns);
            pendingStepIndex++;
            forcedJumpIndex = end;
            return true;
        }

        // The full selected turn has finished.
        TurnMove completedTurn = matchingTurns.getFirst();
        List<Integer> capturedIndices = completedTurn.steps().stream()
                .filter(MoveStep::isCapture)
                .map(MoveStep::capturedIndex)
                .toList();

        rules.finishTurn(board, capturedIndices, currentPlayer);

        // Apply Malaysian huffing after the quiet move is completed.
        if (pendingForfeitIndex >= 0) {
            int actualForfeitIndex = pendingForfeitIndex;

            if (pendingForfeitIndex == turnStartIndex) {
                actualForfeitIndex = completedTurn.getEndIndex();
            }

            if (board.isValidIndex(actualForfeitIndex)
                    && board.isPlayableIndex(actualForfeitIndex)
                    && belongsToPlayer(
                            board.getPiece(actualForfeitIndex), currentPlayer)) {
                board.removePiece(actualForfeitIndex);
                turnHadProgress = true;
            }
        }

        finishCompletedTurn();
        return true;
    }

    /** Checks whether a proposed UI step is legal under the active variant. */
    public boolean isValidMove(int start, int end) {
        if (isGameOver()
                || !board.isValidIndex(start)
                || !board.isValidIndex(end)
                || !board.isPlayableIndex(start)
                || !board.isPlayableIndex(end)
                || !board.isEmpty(end)
                || !isCurrentPlayerPiece(start)) {
            return false;
        }

        if (forcedJumpIndex != -1 && start != forcedJumpIndex) {
            return false;
        }

        return !matchingNextSteps(
                getTurnsForCurrentDecision(), start, end).isEmpty();
    }

    /** Returns every legal destination for a piece at the current step. */
    public List<Integer> getLegalDestinations(int start) {
        if (isGameOver()
                || !board.isValidIndex(start)
                || !board.isPlayableIndex(start)
                || !isCurrentPlayerPiece(start)
                || (forcedJumpIndex != -1 && start != forcedJumpIndex)) {
            return List.of();
        }

        List<Integer> destinations = new ArrayList<>();

        for (TurnMove turn : getTurnsForCurrentDecision()) {
            if (pendingStepIndex >= turn.steps().size()) {
                continue;
            }

            MoveStep step = turn.steps().get(pendingStepIndex);

            if (step.startIndex() == start
                    && !destinations.contains(step.endIndex())) {
                destinations.add(step.endIndex());
            }
        }

        return List.copyOf(destinations);
    }

    /** Checks whether a player has at least one complete legal turn. */
    public boolean hasAnyLegalMove(int player) {
        if (player != red_player && player != black_player) {
            return false;
        }
        return !rules.getLegalTurns(board, player).isEmpty();
    }

    /** Checks whether any available legal turn begins with a capture. */
    public boolean hasAnyJump(int player) {
        if (player != red_player && player != black_player) {
            return false;
        }

        if (player == currentPlayer && forcedJumpIndex != -1) {
            return pendingTurns.stream().anyMatch(turn ->
                    pendingStepIndex < turn.steps().size()
                    && turn.steps().get(pendingStepIndex).isCapture());
        }

        return rules.getLegalTurns(board, player).stream()
                .anyMatch(turn -> turn.steps().getFirst().isCapture());
    }

    /** Checks whether this piece has a legal capture available. */
    private boolean hasJumpFrom(int start) {
        return getTurnsForCurrentDecision().stream().anyMatch(turn ->
                pendingStepIndex < turn.steps().size()
                && turn.steps().get(pendingStepIndex).startIndex() == start
                && turn.steps().get(pendingStepIndex).isCapture());
    }

    /** Counts the remaining pieces belonging to a player. */
    public int countPieces(int player) {
        if (player != red_player && player != black_player) {
            return 0;
        }

        int count = 0;
        for (int index = 0; index < board.getCellCount(); index++) {
            if (board.isPlayableIndex(index)
                    && belongsToPlayer(board.getPiece(index), player)) {
                count++;
            }
        }
        return count;
    }

    private List<TurnMove> getTurnsForCurrentDecision() {
        if (forcedJumpIndex != -1) {
            return pendingTurns;
        }
        return rules.getLegalTurns(board, currentPlayer);
    }

    private List<TurnMove> matchingNextSteps(
            List<TurnMove> turns, int start, int end) {

        List<TurnMove> matches = new ArrayList<>();

        for (TurnMove turn : turns) {
            if (pendingStepIndex >= turn.steps().size()) {
                continue;
            }

            MoveStep step = turn.steps().get(pendingStepIndex);

            if (step.startIndex() == start && step.endIndex() == end) {
                matches.add(turn);
            }
        }

        return matches;
    }

    private void finishCompletedTurn() {
        forcedJumpIndex = -1;
        pendingTurns = List.of();
        pendingStepIndex = 0;
        turnStartIndex = -1;
        pendingForfeitIndex = -1;

        if (turnHadProgress) {
            turnsWithoutProgress = 0;
        } else {
            turnsWithoutProgress++;
        }
        turnHadProgress = false;

        currentPlayer = otherPlayer(currentPlayer);
        updateGameStateAfterCompletedTurn();
    }

    private void updateGameStateAfterCompletedTurn() {
        int playerToMove = currentPlayer;
        int previousPlayer = otherPlayer(playerToMove);

        if (countPieces(playerToMove) == 0
                || !hasAnyLegalMove(playerToMove)) {
            gameState = previousPlayer == red_player
                    ? GameState.RED_WON
                    : GameState.BLACK_WON;
            return;
        }

        int drawLimit = rules.getDrawTurnLimit();
        if (drawLimit > 0 && turnsWithoutProgress >= drawLimit) {
            gameState = GameState.DRAW;
            return;
        }

        int repetitions = positionCounts.merge(
                createPositionKey(), 1, Integer::sum);

        if (repetitions >= 3) {
            gameState = GameState.DRAW;
        }
    }

    private void recordCurrentPosition() {
        positionCounts.merge(createPositionKey(), 1, Integer::sum);
    }

    private String createPositionKey() {
        StringBuilder key = new StringBuilder();

        key.append(getConfig().getVariantId())
                .append(':')
                .append(currentPlayer)
                .append(':');

        for (int index = 0; index < board.getCellCount(); index++) {
            Piece piece = board.getPiece(index);
            key.append(piece == null ? Piece.empty : piece.getType());
        }

        return key.toString();
    }

    private boolean belongsToPlayer(Piece piece, int player) {
        if (piece == null || piece.isEmpty()) {
            return false;
        }
        if (player == red_player) {
            return piece.isRed();
        }
        if (player == black_player) {
            return piece.isBlack();
        }
        return false;
    }

    private int otherPlayer(int player) {
        return player == red_player ? black_player : red_player;
    }
}
