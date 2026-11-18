
package com.checkers.controller;

import com.checkers.Main;
import com.checkers.api.GameApi;
import com.checkers.logic.Board;
import com.checkers.logic.GameLogic;
import com.checkers.rules.GameRules;
import com.checkers.logic.Piece;
import com.checkers.websocket.GameWebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GameBoardController {

    private record MoveFeedback(
            int start,
            int end,
            boolean red,
            boolean wasKing,
            boolean promoted,
            List<Integer> captured) { }

    private boolean onlineMode = false;
    private boolean awaitingServerMove = false;

    private String onlineGameId;
    private int onlinePlayerNumber;
    private GameWebSocketClient webSocketClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double MAX_BOARD_SIZE = 500;

    private GameLogic gameLogic;
    private BorderPane root;
    private GridPane board;
    private Label gameStatus;
        private ReinforcementLearningTrainer.TrainedAgent trainedAgent;
        private boolean aiThinking;
        private GameApi localGameApi;
        private boolean aiOpponent;
        private boolean localGameSaveStarted;
        private final List<Map<String, Object>> localGameMoves = new ArrayList<>();

    private int selectedIndex = -1;

    private List<Integer> legalDestinations = Collections.emptyList();

    private boolean gameOverMessageShown = false;

    public GameBoardController() {
        this(new GameLogic());
    }

    public GameBoardController(GameLogic gameLogic) {
        this(gameLogic, null);
    }

    public GameBoardController(
            GameLogic gameLogic, ReinforcementLearningTrainer.TrainedAgent trainedAgent) {
        if (gameLogic == null) {
            throw new IllegalArgumentException(
                    "GameLogic cannot be null.");
        }

        this.gameLogic = gameLogic;
        this.trainedAgent = trainedAgent;
    }

    /*
     * Checks whether the player selected Senegalese Checkers.
     * This keeps the other variants using their normal colors.
     */
    private boolean isSenegaleseVariant() {
        return "SENEGALESE".equals(
                gameLogic.getConfig().getVariantId());
    }

    /*
     * Senegalese Checkers uses green for the playable squares.
     */
    private Color getDarkSquareColor() {
        return isSenegaleseVariant()
                ? Color.web("#00853F")
                : GameStyleConfig.DARK_SQUARE_COLOR;
    }

    /*
     * Senegalese Checkers uses a light cream color
     * for the remaining squares.
     */
    private Color getLightSquareColor() {
        return isSenegaleseVariant()
                ? Color.web("#FFF4C2")
                : GameStyleConfig.LIGHT_SQUARE_COLOR;
    }

    public Scene createScene() {
        // Displays the name of the selected checkers variant.
        Label title = new Label(
                gameLogic.getRules().getDisplayName());
        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;");

        gameStatus = new Label();
        gameStatus.setStyle("-fx-font-size: 16px;");

        board = createBoard();
        board.setAlignment(Pos.CENTER);

        updateGameStatus();

        Button newGameButton = new Button("New Game");
        AppTheme.stylePrimaryButton(newGameButton);
        newGameButton.setDisable(onlineMode);
        Button backButton = new Button("Back to Menu");
        AppTheme.styleSecondaryButton(backButton);

        Button rulesButton = new Button("Rules / Help");
        AppTheme.styleSecondaryButton(rulesButton);
        rulesButton.setOnAction(event -> showRulesTutorial());

        newGameButton.setOnAction(event -> startNewGame());

        backButton.setOnAction(event -> Main.showMainMenu());

        HBox buttons = new HBox(15, rulesButton, newGameButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        VBox topSection = new VBox(10, title, gameStatus);
        topSection.setAlignment(Pos.CENTER);

        root = new BorderPane();
        root.setPadding(new Insets(25));
        AppTheme.applyBackground(root);
        root.setTop(topSection);
        root.setCenter(board);
        root.setBottom(buttons);

        BorderPane.setMargin(board, new Insets(20, 0, 20, 0));

        return new Scene(root);
    }

    private int getBoardSize() {
        return gameLogic.getBoard().getBoardSize();
    }

    private Board getCurrentBoard() {
        return gameLogic.getBoard();
    }

    private GridPane createBoard() {
        GridPane grid = new GridPane();

        int size = getBoardSize();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                StackPane square = createSquare(row, col);
                grid.add(square, col, row);
            }
        }

        return grid;
    }

    private StackPane createSquare(int row, int col) {
        StackPane square = new StackPane();
        double squareSize = getSquareSize();

        square.setMinSize(squareSize, squareSize);
        square.setPrefSize(squareSize, squareSize);
        square.setMaxSize(squareSize, squareSize);

        Rectangle background = new Rectangle(
            squareSize,
            squareSize);

        background.setStrokeType(
                javafx.scene.shape.StrokeType.INSIDE);

        boolean darkSquare = getCurrentBoard()
                .getConfig()
                .isPlayableSquare(row, col);

        // Uses Senegalese colors only when the Senegalese variant is active.
        background.setFill(
                darkSquare
                        ? getDarkSquareColor()
                        : getLightSquareColor());

        square.getChildren().add(background);

        // IMPORTANT: use the current Board's coordinate mapping.
        // Do not use the legacy static Board.toIndex(row, col).
        int index = getCurrentBoard().indexAt(row, col);

        if (index < 0) {
            return square;
        }

        if (index == selectedIndex) {
            background.setStroke(Color.GOLD);
            background.setStrokeWidth(3);
        } else if (legalDestinations.contains(index)) {
            background.setFill(Color.DARKSEAGREEN);
            background.setStroke(Color.LIMEGREEN);
            background.setStrokeWidth(3);
        }

        Piece piece = getCurrentBoard().getPiece(index);

        if (piece != null && !piece.isEmpty()) {
            Circle pieceCircle = createPiece(piece);

            if (pieceCircle != null) {
                square.getChildren().add(pieceCircle);
            }
        } else if (legalDestinations.contains(index)) {
            Circle moveMarker = new Circle(9, Color.LIMEGREEN);
            moveMarker.setOpacity(0.85);
            square.getChildren().add(moveMarker);
        }

        square.setOnMouseClicked(event -> handleSquareClick(index));

        return square;
    }

    private Circle createPiece(Piece piece) {
        Color color;

        /*
         * Senegalese Checkers uses yellow and red pieces.
         * Other variants continue using their original piece colors.
         */
        if (piece.isBlack()) {
            color = isSenegaleseVariant()
                    ? Color.web("#FCD116")
                    : GameStyleConfig.BLACK_PIECE_COLOR;
        } else if (piece.isRed()) {
            color = isSenegaleseVariant()
                    ? Color.web("#CE1126")
                    : GameStyleConfig.RED_PIECE_COLOR;
        } else {
            return null;
        }

        Circle circle = new Circle(getSquareSize() * 0.35);

        circle.setStrokeType(
                javafx.scene.shape.StrokeType.INSIDE);

        circle.setFill(color);
        circle.setStroke(GameStyleConfig.PIECE_OUTLINE_COLOR);
        circle.setStrokeWidth(2);

        if (piece.isKing()) {
            circle.setStroke(Color.GOLD);
            circle.setStrokeWidth(4);
        }

        return circle;
    }

    private double getSquareSize() {
        return Math.min(GameStyleConfig.SQUARE_SIZE,
                MAX_BOARD_SIZE / getBoardSize());
    }

    private void startNewGame() {
        gameLogic = new GameLogic(gameLogic.getRules());
        selectedIndex = -1;
        legalDestinations = Collections.emptyList();
        gameOverMessageShown = false;
        awaitingServerMove = false;
        localGameSaveStarted = false;
        localGameMoves.clear();
        refreshBoard();
        updateGameStatus();
    }

    private void handleSquareClick(int clickedIndex) {
        if (aiThinking) {
            return;
        }
        if (gameLogic.isGameOver()) {
            showGameOverMessage();
            return;
        }

        if (onlineMode && awaitingServerMove) {
            return;
        }

        if (selectedIndex == -1) {
            if (onlineMode && !isLocalPlayersPiece(clickedIndex)) {
                gameStatus.setText("Select one of your own pieces.");
                return;
            }

            if (gameLogic.isCurrentPlayerPiece(clickedIndex)) {
                List<Integer> destinations = gameLogic.getLegalDestinations(clickedIndex);

                if (destinations.isEmpty()) {
                    gameStatus.setText(
                            gameLogic.hasAnyJump(gameLogic.getCurrentPlayer())
                                    ? "A jump is available. Select a piece that can jump."
                                    : "That piece has no legal moves.");
                } else {
                    selectedIndex = clickedIndex;
                    legalDestinations = destinations;
                    gameStatus.setText("Choose a highlighted destination.");
                    refreshBoard();
                }
            } else {
                gameStatus.setText("Select one of your pieces.");
            }

            return;
        }

        if (selectedIndex == clickedIndex) {
            selectedIndex = -1;
            legalDestinations = Collections.emptyList();
            refreshBoard();
            updateGameStatus();
            return;
        }

        if (onlineMode) {
            Board currentBoard = getCurrentBoard();

            int fromRow = currentBoard.rowOf(selectedIndex);
            int fromCol = currentBoard.colOf(selectedIndex);
            int toRow = currentBoard.rowOf(clickedIndex);
            int toCol = currentBoard.colOf(clickedIndex);

            if (fromRow < 0 || fromCol < 0 || toRow < 0 || toCol < 0) {
                gameStatus.setText("Invalid square selected.");
                return;
            }

            if (!legalDestinations.contains(clickedIndex)) {
                if (gameLogic.isCurrentPlayerPiece(clickedIndex)
                        && isLocalPlayersPiece(clickedIndex)) {
                    List<Integer> destinations = gameLogic.getLegalDestinations(clickedIndex);

                    if (!destinations.isEmpty()) {
                        selectedIndex = clickedIndex;
                        legalDestinations = destinations;
                        refreshBoard();
                        gameStatus.setText("Choose a highlighted destination.");
                    }
                } else {
                    gameStatus.setText("Choose a highlighted destination.");
                }

                return;
            }

            requestMove(fromRow, fromCol, toRow, toCol);
            return;
        }

        Board beforeMove = getCurrentBoard().copy();
        int movingPlayer = gameLogic.getCurrentPlayer();
        if (gameLogic.move(selectedIndex, clickedIndex)) {
            MoveFeedback feedback = createMoveFeedback(
                    beforeMove, selectedIndex, clickedIndex);
            recordLocalMove(selectedIndex, clickedIndex, movingPlayer);
            updateSelectionAfterMove();
            refreshBoard();
            animateMove(feedback, () -> {
                showGameOverMessage();
                requestAiMove();
            });
            updateGameStatus();
            return;
        }

        if (gameLogic.isCurrentPlayerPiece(clickedIndex)) {
            List<Integer> destinations = gameLogic.getLegalDestinations(clickedIndex);

            if (!destinations.isEmpty()) {
                selectedIndex = clickedIndex;
                legalDestinations = destinations;
                gameStatus.setText("Choose a highlighted destination.");
                refreshBoard();
            } else {
                gameStatus.setText("That piece has no legal moves.");
            }
        } else {
            gameStatus.setText("Invalid move. Try again.");
        }
    }

    private void refreshBoard() {
        if (root == null) {
            return;
        }

        board = createBoard();
        board.setAlignment(Pos.CENTER);
        root.setCenter(board);
    }

    private MoveFeedback createMoveFeedback(Board before, int start, int end) {
        Piece movingPiece = before.getPiece(start);
        Piece movedPiece = getCurrentBoard().getPiece(end);
        if (movingPiece == null || movingPiece.isEmpty()
                || movedPiece == null || movedPiece.isEmpty()) {
            return null;
        }

        List<Integer> captured = new ArrayList<>();
        for (int index = 0; index < before.getCellCount(); index++) {
            Piece previous = before.getPiece(index);
            Piece current = getCurrentBoard().getPiece(index);
            if (index != start && previous != null && !previous.isEmpty()
                    && current != null && current.isEmpty()
                    && previous.isRed() != movingPiece.isRed()) {
                captured.add(index);
            }
        }

        return new MoveFeedback(
                start,
                end,
                movingPiece.isRed(),
                movingPiece.isKing(),
                movedPiece.isKing(),
                captured);
    }

    private void animateMove(MoveFeedback feedback) {
        animateMove(feedback, () -> { });
    }

    private void animateMove(MoveFeedback feedback, Runnable onFinished) {
        if (feedback == null || board == null) {
            onFinished.run();
            return;
        }

        int size = getBoardSize();
        int fromRow = getCurrentBoard().rowOf(feedback.start());
        int fromCol = getCurrentBoard().colOf(feedback.start());
        int toRow = getCurrentBoard().rowOf(feedback.end());
        int toCol = getCurrentBoard().colOf(feedback.end());
        StackPane destination = findSquare(toRow, toCol);
        Circle destinationPiece = findPieceCircle(destination);
        if (destinationPiece == null) {
            onFinished.run();
            return;
        }

        Circle movingPiece = new Circle(getSquareSize() * 0.35);
        movingPiece.setFill(feedback.red()
                ? (isSenegaleseVariant()
                        ? Color.web("#CE1126")
                        : GameStyleConfig.RED_PIECE_COLOR)
                : (isSenegaleseVariant()
                        ? Color.web("#FCD116")
                        : GameStyleConfig.BLACK_PIECE_COLOR));
        movingPiece.setStroke(feedback.promoted()
                ? Color.GOLD : GameStyleConfig.PIECE_OUTLINE_COLOR);
        movingPiece.setStrokeWidth(feedback.promoted() ? 4 : 2);
        movingPiece.setMouseTransparent(true);
        board.add(movingPiece, fromCol, fromRow);
        destinationPiece.setOpacity(0);

        TranslateTransition travel = new TranslateTransition(
                Duration.millis(feedback.captured().isEmpty() ? 220 : 300),
                movingPiece);
        travel.setByX((toCol - fromCol) * getSquareSize());
        travel.setByY((toRow - fromRow) * getSquareSize());

        List<FadeTransition> captureEffects = new ArrayList<>();
        for (int capturedIndex : feedback.captured()) {
            int row = getCurrentBoard().rowOf(capturedIndex);
            int col = getCurrentBoard().colOf(capturedIndex);
            StackPane capturedSquare = findSquare(row, col);
            Circle flash = new Circle(getSquareSize() * 0.28);
            flash.setFill(Color.rgb(220, 48, 48, 0.8));
            flash.setMouseTransparent(true);
            capturedSquare.getChildren().add(flash);
            FadeTransition fade = new FadeTransition(Duration.millis(280), flash);
            fade.setFromValue(0.9);
            fade.setToValue(0);
            fade.setOnFinished(event -> capturedSquare.getChildren().remove(flash));
            captureEffects.add(fade);
        }

        ParallelTransition animation = new ParallelTransition();
        animation.getChildren().add(travel);
        animation.getChildren().addAll(captureEffects);
        if (feedback.promoted() || feedback.wasKing()) {
            ScaleTransition flourish = new ScaleTransition(
                    Duration.millis(300), movingPiece);
            flourish.setFromX(1);
            flourish.setFromY(1);
            flourish.setToX(1.25);
            flourish.setToY(1.25);
            flourish.setAutoReverse(true);
            flourish.setCycleCount(2);
            animation.getChildren().add(flourish);
        }
        animation.setOnFinished(event -> {
            board.getChildren().remove(movingPiece);
            destinationPiece.setOpacity(1);
            onFinished.run();
        });
        animation.play();
    }

    private StackPane findSquare(int row, int col) {
        for (var child : board.getChildren()) {
            int childRow = GridPane.getRowIndex(child) == null
                    ? 0 : GridPane.getRowIndex(child);
            int childCol = GridPane.getColumnIndex(child) == null
                    ? 0 : GridPane.getColumnIndex(child);
            if (childRow == row && childCol == col
                    && child instanceof StackPane square) {
                return square;
            }
        }
        return null;
    }

    private Circle findPieceCircle(StackPane square) {
        if (square == null) {
            return null;
        }
        return square.getChildren().stream()
                .filter(Circle.class::isInstance)
                .map(Circle.class::cast)
                .filter(circle -> circle.getRadius() > 9)
                .findFirst()
                .orElse(null);
    }

    private void showRulesTutorial() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("How to Play");
        dialog.setHeaderText(gameLogic.getRules().getDisplayName() + " Rules");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label ruleSummary = new Label(getVariantRulesSummary());
        ruleSummary.setWrapText(true);
        ruleSummary.setStyle("-fx-font-size: 14px; -fx-line-spacing: 3px;");

        Label demoTitle = new Label("Move examples");
        demoTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        GridPane demoBoard = new GridPane();
        demoBoard.setAlignment(Pos.CENTER);
        demoBoard.setHgap(0);
        demoBoard.setVgap(0);
        HBox demos = new HBox(8);
        demos.setAlignment(Pos.CENTER);
        ToggleGroup demoGroup = new ToggleGroup();
        String[] names = {"Move", "Capture", "Become a king", "King move"};
        for (int demo = 0; demo < names.length; demo++) {
            ToggleButton button = new ToggleButton(names[demo]);
            button.setToggleGroup(demoGroup);
            demos.getChildren().add(button);
            if (demo == 0) {
                button.setSelected(true);
            }
        }
        Label demoDescription = new Label();
        demoDescription.setWrapText(true);
        showTutorialDemo(demoBoard, 0, demoDescription);
        for (var button : demos.getChildren()) {
            if (button instanceof ToggleButton toggle) {
                int demoIndex = demos.getChildren().indexOf(button);
                toggle.setOnAction(event ->
                        showTutorialDemo(demoBoard, demoIndex, demoDescription));
            }
        }

        VBox content = new VBox(
                12, ruleSummary, new Separator(), demoTitle, demos,
                demoBoard, demoDescription);
        content.setPadding(new Insets(8));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(500);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(590);
        dialog.showAndWait();
    }

    private String getVariantRulesSummary() {
        String common = "TURKISH".equals(gameLogic.getConfig().getVariantId())
            ? "Take turns moving one piece to an open square. A capture removes an opponent "
                + "piece; reaching the far edge promotes a piece to king.\n\n"
            : "Take turns moving one piece diagonally to an open playable square. "
                + "A jump lands beyond an opponent piece and captures it. "
                + "Pieces reaching the far edge are promoted to kings.\n\n";
        return common + switch (gameLogic.getConfig().getVariantId()) {
            case "AMERICAN" -> "American Checkers: captures are mandatory. Regular pieces move "
                    + "and capture forward; kings move one diagonal square in either direction.";
            case "MALAYSIAN" -> "Malaysian Draughts: kings can travel along a diagonal. "
                    + "Choose a longest available capture sequence; declining a capture can forfeit a piece.";
            case "SENEGALESE" -> "Senegalese Checkers: captures may go backward, kings travel "
                    + "along diagonals, and the greatest available capture sequence is required.";
            case "THAI" -> "Thai Draughts: captures are mandatory and a piece must continue "
                    + "capturing while another jump is available. Promotion ends the capture turn.";
            case "TURKISH" -> "Turkish Draughts uses orthogonal movement rather than diagonal "
                    + "movement. Regular pieces advance forward; kings travel along rows and columns.";
            default -> "Movement and capture details depend on this variant. The highlighted "
                    + "squares on the board show the legal choices for the selected piece.";
        };
    }

    private void showTutorialDemo(
            GridPane demoBoard, int demo, Label description) {
        demoBoard.getChildren().clear();
        StackPane[][] squares = new StackPane[4][4];
        double cellSize = 42;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                StackPane square = new StackPane();
                square.setMinSize(cellSize, cellSize);
                square.setPrefSize(cellSize, cellSize);
                square.setMaxSize(cellSize, cellSize);
                Rectangle background = new Rectangle(cellSize, cellSize);
                background.setFill((row + col) % 2 == 0
                        ? Color.web("#E8D9B5") : Color.web("#76543B"));
                square.getChildren().add(background);
                squares[row][col] = square;
                demoBoard.add(square, col, row);
            }
        }

        int startRow = demo == 2 ? 1 : (demo == 3 ? 2 : 3);
        int startCol = demo == 2 ? 2 : (demo == 3 ? 1 : 0);
        int endRow = demo == 2 ? 0 : (demo == 3 ? 1 : (demo == 1 ? 1 : 2));
        int endCol = demo == 2 ? 3 : (demo == 3 ? 2 : (demo == 1 ? 2 : 1));
        int middleRow = (startRow + endRow) / 2;
        int middleCol = (startCol + endCol) / 2;
        if (demo == 1) {
            Circle opponent = tutorialPiece(false, false);
            squares[middleRow][middleCol].getChildren().add(opponent);
        }

        Circle actor = tutorialPiece(true, demo == 3);
        squares[startRow][startCol].getChildren().add(actor);
        Circle animatedActor = tutorialPiece(true, demo == 3);
        demoBoard.add(animatedActor, startCol, startRow);
        actor.setOpacity(0);

        description.setText(switch (demo) {
            case 1 -> "A capture jumps over an opposing piece; the jumped piece is removed.";
            case 2 -> "Reach the far edge to become a king. The gold rim marks the promotion.";
            case 3 -> "Kings move diagonally. Some variants allow kings to travel several squares.";
            default -> "Choose a highlighted legal destination to move one square.";
        });

        TranslateTransition move = new TranslateTransition(
                Duration.millis(550), animatedActor);
        move.setByX((endCol - startCol) * cellSize);
        move.setByY((endRow - startRow) * cellSize);
        move.setOnFinished(event -> {
            demoBoard.getChildren().remove(animatedActor);
            if (demo == 1) {
                squares[middleRow][middleCol].getChildren().remove(1);
            }
            squares[startRow][startCol].getChildren().remove(actor);
            animatedActor.setTranslateX(0);
            animatedActor.setTranslateY(0);
            if (demo == 2) {
                animatedActor.setStroke(Color.GOLD);
                animatedActor.setStrokeWidth(4);
                ScaleTransition crown = new ScaleTransition(
                        Duration.millis(220), animatedActor);
                crown.setFromX(1);
                crown.setFromY(1);
                crown.setToX(1.25);
                crown.setToY(1.25);
                crown.setAutoReverse(true);
                crown.setCycleCount(2);
                crown.play();
            }
            squares[endRow][endCol].getChildren().add(animatedActor);
        });
        move.play();
    }

    private Circle tutorialPiece(boolean red, boolean king) {
        Circle piece = new Circle(13);
        piece.setFill(red ? Color.web("#CE1126") : Color.web("#242424"));
        piece.setStroke(king ? Color.GOLD : Color.WHITE);
        piece.setStrokeWidth(king ? 4 : 2);
        return piece;
    }

    private void updateSelectionAfterMove() {
        if (gameLogic.isMultiJumpInProgress()) {
            selectedIndex = gameLogic.getForcedJumpIndex();
            legalDestinations = gameLogic.getLegalDestinations(selectedIndex);
        } else {
            selectedIndex = -1;
            legalDestinations = Collections.emptyList();
        }
    }

    private void updateGameStatus() {
        if (gameStatus == null || gameLogic == null) {
            return;
        }

        if (gameLogic.isGameOver()) {
            gameStatus.setText(gameLogic.getGameResultMessage());
            return;
        }

        if (gameLogic.isMultiJumpInProgress()) {
            gameStatus.setText("Jump again with the highlighted piece.");
            return;
        }

        if (gameLogic.getCurrentPlayer() == GameLogic.red_player) {
            gameStatus.setText("Red player's turn");
        } else {
            gameStatus.setText("Black player's turn");
        }
    }

    private void requestAiMove() {
        if (!ReinforcementLearningTrainer.shouldPlayAsAi(
                gameLogic, trainedAgent, onlineMode, aiThinking)) {
            return;
        }

        aiThinking = true;
        gameStatus.setText("AI is thinking...");
        GameLogic source = gameLogic;
        GameLogic position = source.copy();
        ReinforcementLearningTrainer.TrainedAgent agent = trainedAgent;
        Thread worker = new Thread(() -> {
            try {
                var turn = ReinforcementLearningTrainer.selectMove(agent, position);
                Platform.runLater(() -> {
                    if (trainedAgent != agent
                            || gameLogic != source || turn == null || gameLogic.isGameOver()) {
                        aiThinking = false;
                        return;
                    }
                    applyAiTurnStep(turn.steps(), 0, agent, source);
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    aiThinking = false;
                    gameStatus.setText("AI failed: " + exception.getMessage());
                });
            }
        }, "checkers-ai-move");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyAiTurnStep(
            List<com.checkers.logic.Move.MoveStep> steps,
            int stepIndex,
            ReinforcementLearningTrainer.TrainedAgent agent,
            GameLogic source) {
        if (trainedAgent != agent || gameLogic != source) {
            aiThinking = false;
            return;
        }
        if (gameLogic.isGameOver()) {
            aiThinking = false;
            updateGameStatus();
            showGameOverMessage();
            return;
        }
        if (stepIndex >= steps.size()) {
            aiThinking = false;
            updateGameStatus();
            showGameOverMessage();
            return;
        }

        var step = steps.get(stepIndex);
        int movingPlayer = gameLogic.getCurrentPlayer();
        Board beforeMove = getCurrentBoard().copy();
        if (!gameLogic.move(step.startIndex(), step.endIndex())) {
            aiThinking = false;
            gameStatus.setText("AI move could not be applied.");
            return;
        }
        MoveFeedback feedback = createMoveFeedback(
                beforeMove, step.startIndex(), step.endIndex());
        recordLocalMove(step.startIndex(), step.endIndex(), movingPlayer);
        updateSelectionAfterMove();
        refreshBoard();
        animateMove(feedback, () -> {
            PauseTransition nextStep = new PauseTransition(Duration.millis(120));
            nextStep.setOnFinished(event ->
                applyAiTurnStep(steps, stepIndex + 1, agent, source));
            nextStep.play();
        });
        updateGameStatus();
    }

    private void showGameOverMessage() {
        if (!gameLogic.isGameOver() || gameOverMessageShown) {
            return;
        }

        gameOverMessageShown = true;
        saveCompletedLocalGame();

        boolean draw = gameLogic.getGameState() == GameLogic.GameState.DRAW;
        boolean redWon = gameLogic.getGameState() == GameLogic.GameState.RED_WON;
        boolean localPlayerWon = onlineMode
                ? (onlinePlayerNumber == 1) == redWon
                : !aiOpponent || redWon;
        String headline = draw
                ? "Draw"
                : onlineMode || aiOpponent
                        ? (localPlayerWon ? "Victory!" : "Defeat")
                        : (redWon ? "Red wins!" : "Black wins!");

        Label result = new Label(headline);
        result.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        Label details = new Label(gameLogic.getGameResultMessage());
        details.setStyle("-fx-font-size: 16px;");
        VBox resultContent = new VBox(12, result, details);
        resultContent.setAlignment(Pos.CENTER);
        resultContent.setPadding(new Insets(24, 40, 24, 40));

        ButtonType playAgain = new ButtonType("New Game", ButtonBar.ButtonData.OK_DONE);
        ButtonType mainMenu = new ButtonType("Main Menu", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Game Over");
        dialog.getDialogPane().getButtonTypes().addAll(playAgain, mainMenu);
        dialog.getDialogPane().setContent(resultContent);
        dialog.setOnShown(event -> {
            result.setScaleX(0.7);
            result.setScaleY(0.7);
            result.setOpacity(0);
            ScaleTransition scale = new ScaleTransition(Duration.millis(420), result);
            scale.setToX(1);
            scale.setToY(1);
            FadeTransition fade = new FadeTransition(Duration.millis(420), result);
            fade.setToValue(1);
            new ParallelTransition(scale, fade).play();
        });
        dialog.showAndWait().ifPresent(choice -> {
            if (choice == playAgain) {
                startNewGame();
            } else if (choice == mainMenu) {
                Main.showMainMenu();
            }
        });
    }

    public void configureLocalPersistence(GameApi gameApi, LocalGameOptions options) {
        if (gameApi == null || options == null) {
            throw new IllegalArgumentException("Local game persistence settings are required.");
        }
        localGameApi = gameApi;
        aiOpponent = options.opponent() == LocalGameOptions.Opponent.AI;
    }

    private void recordLocalMove(int start, int end, int movingPlayer) {
        if (onlineMode || localGameApi == null) {
            return;
        }
        Board currentBoard = gameLogic.getBoard();
        int fromRow = currentBoard.rowOf(start);
        int fromCol = currentBoard.colOf(start);
        int toRow = currentBoard.rowOf(end);
        int toCol = currentBoard.colOf(end);
        if (fromRow < 0 || fromCol < 0 || toRow < 0 || toCol < 0) {
            throw new IllegalStateException("A completed move has invalid board coordinates.");
        }

        Map<String, Object> move = new LinkedHashMap<>();
        move.put("version", 1);
        move.put("moveNumber", localGameMoves.size() + 1);
        move.put("start", start);
        move.put("end", end);
        move.put("fromRow", fromRow);
        move.put("fromCol", fromCol);
        move.put("toRow", toRow);
        move.put("toCol", toCol);
        move.put("player", movingPlayer);
        move.put("nextPlayer", gameLogic.getCurrentPlayer());
        move.put("turnComplete", !gameLogic.isMultiJumpInProgress());
        move.put("gameState", gameLogic.getGameState().name());
        move.put("opponent", aiOpponent ? "AI" : "HUMAN");
        localGameMoves.add(move);
    }

    private void saveCompletedLocalGame() {
        if (onlineMode || localGameApi == null || localGameSaveStarted) {
            return;
        }
        localGameSaveStarted = true;
        if (localGameMoves.isEmpty()) {
            gameStatus.setText("Game over. No move history was available to save.");
            return;
        }

        Integer winnerPlayer = switch (gameLogic.getGameState()) {
            case RED_WON -> 1;
            case BLACK_WON -> 2;
            case DRAW -> null;
            case IN_PROGRESS -> throw new IllegalStateException(
                    "Cannot save a game that is still in progress.");
        };
        List<Map<String, Object>> completedMoves = new ArrayList<>();
        for (Map<String, Object> move : localGameMoves) {
            completedMoves.add(new LinkedHashMap<>(move));
        }
        completedMoves.getLast().put("result", gameLogic.getGameState().name());
        String opponent = aiOpponent ? "AI" : "HUMAN";
        gameStatus.setText("Game over. Saving game...");

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode saved = localGameApi.saveCompletedLocalGame(
                        gameLogic.getConfig().getVariantId(),
                        opponent,
                        gameLogic.getGameState().name(),
                        winnerPlayer,
                        completedMoves);
                long databaseGameId = saved.path("databaseGameId").asLong(-1);
                if (databaseGameId <= 0) {
                    throw new IllegalStateException(
                            "The server did not return a database game ID.");
                }
                Platform.runLater(() -> gameStatus.setText(
                        gameLogic.getGameResultMessage()
                                + " Saved game ID: " + databaseGameId));
            } catch (Exception exception) {
                System.err.println("Could not save completed local game: "
                        + exception.getMessage());
                Platform.runLater(() -> {
                    gameStatus.setText("Game over. Saving to the database failed.");
                    Alert saveError = new Alert(Alert.AlertType.ERROR);
                    saveError.setTitle("Game save failed");
                    saveError.setHeaderText("The completed game was not saved.");
                    saveError.setContentText(exception.getMessage());
                    saveError.show();
                });
            }
        });
    }

    public void configureOnlineGame(
            String gameId,
            int playerNumber,
            GameWebSocketClient client) {
        onlineMode = true;
        trainedAgent = null;
        onlineGameId = gameId;
        onlinePlayerNumber = playerNumber;
        webSocketClient = client;

        awaitingServerMove = false;
        selectedIndex = -1;
        legalDestinations = Collections.emptyList();
    }

    private boolean isLocalPlayersPiece(int index) {
        Piece piece = getCurrentBoard().getPiece(index);

        if (piece == null || piece.isEmpty()) {
            return false;
        }

        if (onlinePlayerNumber == 1) {
            return piece.isRed();
        }

        if (onlinePlayerNumber == 2) {
            return piece.isBlack();
        }

        return false;
    }

    public void handleServerMessage(JsonNode message) {
        try {
            String type = message.path("type").asText("");

            if ("GAME_MESSAGE".equals(type)) {
                int senderPlayer = message.path("player").asInt(-1);
                JsonNode payload = message.path("message");

                if (payload.isTextual()) {
                    message = objectMapper.readTree(payload.asText());
                } else if (payload.isObject()) {
                    message = payload;
                } else {
                    System.err.println(
                            "GAME_MESSAGE has an invalid payload: " + payload);
                    return;
                }

                if (message instanceof ObjectNode objectNode) {
                    objectNode.put("player", senderPlayer);
                }

                type = message.path("type").asText("");
            }

            System.out.println("Handling server message type: " + type);

            switch (type) {
                case "CONNECTED" ->
                    System.out.println("CONNECTED received: " + message);

                case "GAME_STARTED" ->
                    System.out.println("Game started.");

                case "GAME_FINISHED" -> {
                    long databaseGameId = message.path("databaseGameId").asLong(-1);
                    if (databaseGameId > 0) {
                        String finishMessage = message.path("message").asText();
                        Platform.runLater(() -> gameStatus.setText(
                                finishMessage
                                        + " Database game ID: " + databaseGameId));
                    }
                }

                case "PERSISTENCE_ERROR" -> {
                    long databaseGameId = message.path("databaseGameId").asLong(-1);
                    Platform.runLater(() -> gameStatus.setText(
                            "Game ended, but its completion was not saved. Database game ID: "
                                    + databaseGameId));
                }

                case "RECONNECT_TIMEOUT" -> {
                    long databaseGameId = message.path("databaseGameId").asLong(-1);
                    boolean databaseAbandoned = message.path("databaseAbandoned").asBoolean();
                    String timeoutMessage = message.path("message").asText();
                    String persistenceStatus = databaseAbandoned
                            ? "Saved as abandoned. Database game ID: " + databaseGameId
                            : "Could not save abandoned status for game ID: "
                                    + databaseGameId;
                    Platform.runLater(() -> gameStatus.setText(
                            timeoutMessage + " " + persistenceStatus));
                }

                case "PLAYER_JOINED" ->
                    System.out.println("Player joined: " + message);

                case "MOVE" ->
                    handleOnlineMove(message);

                case "PLAYER_LEFT" ->
                    System.out.println("Player left: " + message);

                case "ERROR" -> {
                    awaitingServerMove = false;
                    System.err.println("Server error: " + message);
                    Platform.runLater(() -> gameStatus.setText("Server rejected the move."));
                }

                default ->
                    System.out.println("Unhandled server message: " + message);
            }
        } catch (Exception e) {
            System.err.println(
                    "Failed to handle server message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleOnlineMove(JsonNode message) {
        int movePlayer = message.path("player").asInt(-1);

        Platform.runLater(() -> {
            try {
                JsonNode moveData = message.path("message");

                if (moveData.isTextual()) {
                    moveData = objectMapper.readTree(moveData.asText());
                }

                int fromRow = moveData.path("fromRow").asInt(-1);
                int fromCol = moveData.path("fromCol").asInt(-1);
                int toRow = moveData.path("toRow").asInt(-1);
                int toCol = moveData.path("toCol").asInt(-1);

                Board currentBoard = getCurrentBoard();
                int size = currentBoard.getBoardSize();

                if (!validCoordinate(fromRow, fromCol, size)
                        || !validCoordinate(toRow, toCol, size)) {
                    System.err.println("Invalid move coordinates received.");
                    return;
                }

                int start = currentBoard.indexAt(fromRow, fromCol);
                int end = currentBoard.indexAt(toRow, toCol);

                if (start < 0 || end < 0
                        || !currentBoard.isPlayableIndex(start)
                        || !currentBoard.isPlayableIndex(end)) {
                    System.err.println(
                            "Server sent invalid or unplayable coordinates.");
                    return;
                }

                Board beforeMove = currentBoard.copy();
                boolean moved = gameLogic.move(start, end);

                if (moved) {
                    MoveFeedback feedback = createMoveFeedback(beforeMove, start, end);
                    selectedIndex = -1;
                    legalDestinations = Collections.emptyList();

                    refreshBoard();
                    animateMove(feedback, this::showGameOverMessage);
                    updateGameStatus();

                    System.out.println("Online move applied successfully.");
                } else {
                    System.err.println(
                            "Online move could not be applied locally.");
                }
            } catch (Exception e) {
                System.err.println(
                        "Error processing online move: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (movePlayer == onlinePlayerNumber) {
                    awaitingServerMove = false;
                }
            }
        });
    }

    private boolean validCoordinate(int row, int col, int size) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    private void requestMove(
            int fromRow,
            int fromCol,
            int toRow,
            int toCol) {
        if (!onlineMode) {
            Board currentBoard = getCurrentBoard();

            int start = currentBoard.indexAt(fromRow, fromCol);
            int end = currentBoard.indexAt(toRow, toCol);

            int movingPlayer = gameLogic.getCurrentPlayer();
            Board beforeMove = currentBoard.copy();
            if (start >= 0 && end >= 0
                    && gameLogic.move(start, end)) {
                MoveFeedback feedback = createMoveFeedback(beforeMove, start, end);
                recordLocalMove(start, end, movingPlayer);
                updateSelectionAfterMove();
                refreshBoard();
                animateMove(feedback, this::showGameOverMessage);
                updateGameStatus();
            }

            return;
        }

        if (awaitingServerMove) {
            return;
        }

        if (webSocketClient == null || onlineGameId == null) {
            gameStatus.setText("WebSocket connection is not configured.");
            return;
        }

        awaitingServerMove = true;

        webSocketClient.sendMove(
                onlineGameId,
                fromRow,
                fromCol,
                toRow,
                toCol);

        selectedIndex = -1;
        legalDestinations = Collections.emptyList();
        refreshBoard();
    }
}
