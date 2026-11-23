package com.checkers.controller;

import com.checkers.Main;
import com.checkers.api.GameApi;
import com.checkers.model.User;
import com.checkers.session.SessionManager;
import com.checkers.localization.AppText;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.ComboBox;
import javafx.util.Duration;

public class ReplayController {

        private final GameApi gameApi;

        // Stores the selected game's database ID.
        private long selectedGameId = -1;

        // Stores the position currently displayed on the replay board.
        private int currentReplayPosition = 0;

        // Stores the total number of moves in the selected replay.
        private int replayMoveCount = 0;

        // Automatically advances the replay while it is playing.
        private Timeline replayTimeline;

        // Stores the selected playback speed.
        private double replaySpeed = 1.0;

        // Prevents multiple replay positions from loading at the same time.
        private boolean replayPositionLoading = false;

        // Stores the API used to retrieve the player's saved games.
        public ReplayController(GameApi gameApi) {
                this.gameApi = gameApi;
        }

        // Creates the screen that displays the player's completed games.
        public Scene createScene() {

                Label title = new Label(AppText.text("Game History and Replays"));
                title.setStyle(
                                "-fx-font-size: 30px;"
                                                + "-fx-font-weight: bold;"
                                                + "-fx-text-fill: " + AppTheme.INK + ";");

                Label instructions = new Label(
                                AppText.text("Select a completed game to watch its replay."));
                instructions.setStyle(
                                "-fx-font-size: 16px;"
                                                + "-fx-text-fill: " + AppTheme.MUTED + ";");

                Label statusLabel = new Label(AppText.text("Loading your completed games..."));
                statusLabel.setStyle(
                                "-fx-font-size: 14px;"
                                                + "-fx-text-fill: " + AppTheme.MUTED + ";");

                // Creates the table that displays the saved games.
                TableView<ReplayGame> historyTable = createHistoryTable();
                historyTable.setPlaceholder(
                                new Label(AppText.text("No completed games were found.")));

                // This button will open the replay viewer in the next step.
                Button watchReplayButton = new Button(AppText.text("Watch Selected Replay"));
                AppTheme.stylePrimaryButton(watchReplayButton);
                watchReplayButton.setDisable(true);

                // Enables the replay button after the user selects a game.
                historyTable.getSelectionModel()
                                .selectedItemProperty()
                                .addListener((observable, oldGame, selectedGame) -> {
                                        if (selectedGame != null) {
                                                selectedGameId = selectedGame.gameId();
                                                watchReplayButton.setDisable(false);

                                                statusLabel.setText(
                                                                AppText.text("Selected Game #{0}", selectedGameId));
                                        }
                                });

                // Opens the replay viewer for the selected game.
                watchReplayButton.setOnAction(event -> {
                        if (selectedGameId <= 0) {
                                statusLabel.setText("Please select a game first.");
                                return;
                        }

                        statusLabel.setText(
                                        "Opening replay for Game #" + selectedGameId + "...");

                        // Replaces the history screen with the replay viewer.
                        watchReplayButton.getScene().setRoot(
                                        createReplayViewer());
                });

                Button refreshButton = new Button(AppText.text("Refresh History"));
                AppTheme.styleSecondaryButton(refreshButton);

                // Reloads the completed games from the database.
                refreshButton.setOnAction(event -> {
                        selectedGameId = -1;
                        watchReplayButton.setDisable(true);
                        loadHistory(historyTable, statusLabel);
                });

                Button backButton = new Button(AppText.text("Back to Main Menu"));
                AppTheme.styleSecondaryButton(backButton);
                backButton.setOnAction(event -> Main.showMainMenu());

                HBox buttonRow = new HBox(
                                12,
                                backButton,
                                refreshButton,
                                watchReplayButton);
                buttonRow.setAlignment(Pos.CENTER);

                VBox header = new VBox(8);
                header.setAlignment(Pos.CENTER);
                header.getChildren().addAll(title, instructions);

                VBox centerContent = new VBox(
                                15,
                                historyTable,
                                statusLabel);
                centerContent.setAlignment(Pos.CENTER);

                BorderPane root = new BorderPane();
                root.setPadding(new Insets(30));
                AppTheme.applyBackground(root);

                root.setTop(header);
                root.setCenter(centerContent);
                root.setBottom(buttonRow);

                BorderPane.setMargin(
                                centerContent,
                                new Insets(25, 0, 25, 0));

                // Loads the player's completed games when the screen opens.
                loadHistory(historyTable, statusLabel);

                return new Scene(root, 900, 760);
        }

        // Creates the columns used by the game-history table.
        private TableView<ReplayGame> createHistoryTable() {

                TableView<ReplayGame> table = new TableView<>();
                table.setPrefHeight(500);
                table.setColumnResizePolicy(
                                TableView.CONSTRAINED_RESIZE_POLICY);

                TableColumn<ReplayGame, String> dateColumn = new TableColumn<>(AppText.text("Date"));
                dateColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                                game.getValue().date()));

                TableColumn<ReplayGame, String> idColumn = new TableColumn<>(AppText.text("Game ID"));
                idColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                                String.valueOf(game.getValue().gameId())));

                TableColumn<ReplayGame, String> opponentColumn = new TableColumn<>(AppText.text("Opponent"));
                opponentColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                                game.getValue().opponent()));

                TableColumn<ReplayGame, String> variantColumn = new TableColumn<>(AppText.text("Variant"));
                variantColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                                game.getValue().variant()));

                TableColumn<ReplayGame, String> outcomeColumn = new TableColumn<>(AppText.text("Result"));
                outcomeColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                                game.getValue().outcome()));

                table.getColumns().addAll(
                                dateColumn,
                                idColumn,
                                opponentColumn,
                                variantColumn,
                                outcomeColumn);

                return table;
        }

        // Retrieves the logged-in player's completed games.
        private void loadHistory(
                        TableView<ReplayGame> historyTable,
                        Label statusLabel) {

                User currentUser = SessionManager.getCurrentUser();

                if (currentUser == null || currentUser.getId() == null) {
                        statusLabel.setText(
                                        AppText.text("Your game history could not be loaded."));
                        return;
                }

                historyTable.getItems().clear();
                historyTable.setDisable(true);
                statusLabel.setText(AppText.text("Loading your completed games..."));

                // Runs the API request away from the JavaFX screen.
                CompletableFuture
                                .supplyAsync(() -> {
                                        try {
                                                return gameApi.getCompletedGamesForPlayer(
                                                                currentUser.getId());
                                        } catch (Exception exception) {
                                                throw new RuntimeException(exception);
                                        }
                                })
                                .thenAccept(history -> Platform.runLater(() -> {
                                        if (history == null || !history.isArray()) {
                                                historyTable.setDisable(false);
                                                String response = String.valueOf(history);
                                                System.err.println(
                                                                "Invalid game history response: " + response);
                                                String preview = response.length() > 180
                                                                ? response.substring(0, 180) + "..."
                                                                : response;
                                                statusLabel.setText(
                                                                AppText.text("Invalid server response (expected an array): {0}",
                                                                                preview));
                                                return;
                                        }

                                        addGamesToTable(historyTable, history);
                                        historyTable.setDisable(false);

                                        if (historyTable.getItems().isEmpty()) {
                                                statusLabel.setText(
                                                                AppText.text("You do not have any completed games yet."));
                                        } else {
                                                statusLabel.setText(
                                                                AppText.text("Select a game from the table."));
                                        }
                                }))
                                .exceptionally(exception -> {
                                        Throwable cause = exception;
                                        while (cause.getCause() != null) {
                                                cause = cause.getCause();
                                        }
                                        String failureMessage = cause.getMessage();
                                        if (failureMessage == null || failureMessage.isBlank()) {
                                                failureMessage = cause.getClass().getSimpleName();
                                        }
                                        String reportedFailure = failureMessage;
                                        System.err.println(
                                                        "Could not load game history: " + reportedFailure);

                                        Platform.runLater(() -> {
                                                historyTable.setDisable(false);
                                                statusLabel.setText(AppText.text(
                                                                "Could not load your game history: {0}",
                                                                reportedFailure));
                                        });

                                        return null;
                                });
        }

        // Converts each game from the API into a table row.
        private void addGamesToTable(
                        TableView<ReplayGame> historyTable,
                        JsonNode history) {

                if (history == null || !history.isArray()) {
                        return;
                }

                for (JsonNode game : history) {
                        long gameId = game.path("gameId").asLong(-1);

                        if (gameId <= 0) {
                                continue;
                        }

                        String date = formatDate(
                                        game.path("startedAt").asText(""));

                        String opponent = game.path("opponent")
                                        .asText("Unknown");
                        opponent = translateGameField(opponent);

                        String variant = game.path("variant")
                                        .asText("Unknown");
                        variant = AppText.text(variant);

                        String outcome = game.path("outcome")
                                        .asText("Unknown");
                        outcome = AppText.text(outcome);

                        historyTable.getItems().add(
                                        new ReplayGame(
                                                        gameId,
                                                        date,
                                                        opponent,
                                                        variant,
                                                        outcome));
                }
        }

        // Shortens the database date so it is easier to read.
        private String formatDate(String startedAt) {

                if (startedAt == null || startedAt.isBlank()) {
                        return AppText.text("Unknown");
                }

                String readableDate = startedAt.replace("T", " ");

                if (readableDate.length() > 16) {
                        readableDate = readableDate.substring(0, 16);
                }

                return readableDate;
        }

        private String translateGameField(String value) {
                return switch (value) {
                        case "AI" -> AppText.text("AI");
                        case "HUMAN" -> AppText.text("Human");
                        default -> AppText.text(value);
                };
        }

        // Creates the screen used to watch a completed game.
        private BorderPane createReplayViewer() {
                Label title = new Label(
                                "Replay - Game #" + selectedGameId);

                title.setStyle(
                                "-fx-font-size: 30px;"
                                                + "-fx-font-weight: bold;"
                                                + "-fx-text-fill: " + AppTheme.INK + ";");

                Label moveLabel = new Label("Loading replay...");
                moveLabel.setStyle(
                                "-fx-font-size: 18px;"
                                                + "-fx-text-fill: " + AppTheme.MUTED + ";");

                // Holds the checkerboard returned by the replay API.
                StackPane boardArea = new StackPane();

                boardArea.setPrefSize(520, 520);
                boardArea.setMaxSize(520, 520);

                boardArea.setStyle(
                                "-fx-background-color: white;"
                                                + "-fx-border-color: " + AppTheme.MUTED + ";"
                                                + "-fx-border-width: 2px;");

                Label boardMessage = new Label(
                                "Loading replay board...");

                boardMessage.setStyle(
                                "-fx-font-size: 20px;"
                                                + "-fx-text-fill: " + AppTheme.MUTED + ";");

                boardArea.getChildren().add(boardMessage);

                Button previousButton = new Button("Previous Move");
                Button playButton = new Button("Play");
                Button nextButton = new Button("Next Move");

                AppTheme.styleSecondaryButton(previousButton);
                AppTheme.stylePrimaryButton(playButton);
                AppTheme.styleSecondaryButton(nextButton);

                // Controls stay disabled until the replay finishes loading.
                previousButton.setDisable(true);
                playButton.setDisable(true);
                nextButton.setDisable(true);

                // Loads the previous saved board position.
                previousButton.setOnAction(event -> {
                        if (!replayPositionLoading && currentReplayPosition > 0) {
                                stopReplay(playButton);

                                loadReplayPosition(
                                                currentReplayPosition - 1,
                                                boardArea,
                                                moveLabel,
                                                previousButton,
                                                playButton,
                                                nextButton);
                        }
                });

                // Starts or pauses automatic replay playback.
                playButton.setOnAction(event -> {
                        if (replayTimeline == null) {
                                startReplay(
                                                boardArea,
                                                moveLabel,
                                                previousButton,
                                                playButton,
                                                nextButton);
                        } else {
                                stopReplay(playButton);
                        }
                });

                // Loads the next saved board position.
                nextButton.setOnAction(event -> {
                        if (!replayPositionLoading
                                        && currentReplayPosition < replayMoveCount) {

                                stopReplay(playButton);

                                loadReplayPosition(
                                                currentReplayPosition + 1,
                                                boardArea,
                                                moveLabel,
                                                previousButton,
                                                playButton,
                                                nextButton);
                        }
                });
                // Lets the player choose the replay playback speed.
                ComboBox<String> speedSelector = new ComboBox<>();

                speedSelector.getItems().addAll(
                                "0.5x",
                                "1x",
                                "1.5x",
                                "2x");

                // Uses normal speed when the replay screen first opens.
                speedSelector.setValue("1x");

                // Changes the replay speed when a new option is selected.
                speedSelector.setOnAction(event -> {
                        replaySpeed = switch (speedSelector.getValue()) {
                                case "0.5x" -> 0.5;
                                case "1.5x" -> 1.5;
                                case "2x" -> 2.0;
                                default -> 1.0;
                        };

                        // Restarts playback immediately when the speed changes.
                        if (replayTimeline != null) {
                                startReplay(
                                                boardArea,
                                                moveLabel,
                                                previousButton,
                                                playButton,
                                                nextButton);
                        }
                });

                HBox replayControls = new HBox(
                                15,
                                previousButton,
                                playButton,
                                nextButton,
                                speedSelector);

                replayControls.setAlignment(Pos.CENTER);

                Button backButton = new Button("Back to Game History");
                AppTheme.styleSecondaryButton(backButton);

                // Returns the player to the completed-game table.
                backButton.setOnAction(event -> Main.showReplays());

                VBox content = new VBox(
                                18,
                                title,
                                moveLabel,
                                boardArea,
                                replayControls,
                                backButton);

                content.setAlignment(Pos.CENTER);
                content.setPadding(new Insets(30));

                BorderPane root = new BorderPane(content);
                AppTheme.applyBackground(root);

                // Loads the starting position of the selected replay.
                loadReplayPosition(
                                0,
                                boardArea,
                                moveLabel,
                                previousButton,
                                playButton,
                                nextButton);

                return root;
        }

        // Retrieves one replay position without freezing the JavaFX screen.
        private void loadReplayPosition(
                        int position,
                        StackPane boardArea,
                        Label moveLabel,
                        Button previousButton,
                        Button playButton,
                        Button nextButton) {

                if (replayPositionLoading) {
                        return;
                }

                replayPositionLoading = true;
                moveLabel.setText("Loading move " + position + "...");

                previousButton.setDisable(true);
                nextButton.setDisable(true);

                CompletableFuture
                                .supplyAsync(() -> {
                                        try {
                                                return gameApi.getReplayPosition(
                                                                selectedGameId,
                                                                position);
                                        } catch (Exception exception) {
                                                throw new RuntimeException(exception);
                                        }
                                })
                                .thenAccept(response -> Platform.runLater(() -> {
                                        replayPositionLoading = false;

                                        currentReplayPosition = response
                                                        .path("position")
                                                        .asInt(position);

                                        replayMoveCount = response
                                                        .path("moveCount")
                                                        .asInt(0);

                                        String variantId = response
                                                        .path("variantId")
                                                        .asText("AMERICAN");

                                        String currentPlayer = response
                                                        .path("currentPlayer")
                                                        .asInt(1) == 1
                                                                        ? "Red"
                                                                        : "Black";

                                        String gameState = response
                                                        .path("gameState")
                                                        .asText("IN_PROGRESS");

                                        JsonNode board = response.path("board");

                                        // Draws the saved pieces on the replay board.
                                        drawReplayBoard(
                                                        boardArea,
                                                        board,
                                                        variantId);

                                        String positionText = "Move " + currentReplayPosition
                                                        + " of " + replayMoveCount;

                                        if (currentReplayPosition < replayMoveCount) {

                                                positionText += " - " + currentPlayer
                                                                + " player's turn";
                                        } else if (!"IN_PROGRESS".equals(gameState)) {
                                                positionText += " - " + formatGameState(gameState);
                                        }

                                        moveLabel.setText(positionText);

                                        previousButton.setDisable(
                                                        currentReplayPosition <= 0);

                                        nextButton.setDisable(
                                                        currentReplayPosition >= replayMoveCount);

                                        playButton.setDisable(
                                                        replayMoveCount <= 0);

                                        // Stops playback when the final move is reached.
                                        if (currentReplayPosition >= replayMoveCount) {

                                                stopReplay(playButton);
                                        }
                                }))
                                .exceptionally(error -> {
                                        Platform.runLater(() -> {
                                                replayPositionLoading = false;
                                                stopReplay(playButton);

                                                boardArea.getChildren().clear();

                                                Label errorLabel = new Label(
                                                                "Could not load this replay.");

                                                errorLabel.setStyle(
                                                                "-fx-font-size: 18px;"
                                                                                + "-fx-text-fill: #B83B2D;");

                                                boardArea.getChildren().add(errorLabel);
                                                moveLabel.setText("Replay unavailable.");

                                                previousButton.setDisable(true);
                                                playButton.setDisable(true);
                                                nextButton.setDisable(true);
                                        });

                                        return null;
                                });
        }

        // Automatically moves through the replay until the final position.
        private void startReplay(
                        StackPane boardArea,
                        Label moveLabel,
                        Button previousButton,
                        Button playButton,
                        Button nextButton) {

                if (replayMoveCount <= 0) {
                        return;
                }

                // Starts over when Play is pressed at the end.
                if (currentReplayPosition >= replayMoveCount) {
                        currentReplayPosition = 0;

                        loadReplayPosition(
                                        0,
                                        boardArea,
                                        moveLabel,
                                        previousButton,
                                        playButton,
                                        nextButton);
                }

                stopReplay(playButton);

                double delay = 1000.0 / replaySpeed;

                replayTimeline = new Timeline(
                                new KeyFrame(
                                                Duration.millis(delay),
                                                event -> {
                                                        if (!replayPositionLoading
                                                                        && currentReplayPosition < replayMoveCount) {

                                                                loadReplayPosition(
                                                                                currentReplayPosition + 1,
                                                                                boardArea,
                                                                                moveLabel,
                                                                                previousButton,
                                                                                playButton,
                                                                                nextButton);
                                                        }

                                                        if (currentReplayPosition >= replayMoveCount) {

                                                                stopReplay(playButton);
                                                        }
                                                }));

                replayTimeline.setCycleCount(
                                Animation.INDEFINITE);

                replayTimeline.play();
                playButton.setText("Pause");
        }

        // Stops automatic playback and returns the button to Play.
        private void stopReplay(Button playButton) {
                if (replayTimeline != null) {
                        replayTimeline.stop();
                        replayTimeline = null;
                }

                playButton.setText("Play");
        }

        // Converts the stored game state into text for the replay screen.
        private String formatGameState(String gameState) {
                return switch (gameState) {
                        case "RED_WON" -> "Red won";
                        case "BLACK_WON" -> "Black won";
                        case "TIE" -> "Game ended in a tie";
                        default -> "Game complete";
                };
        }

        // Draws the replay board using the piece values returned by the API.
        private void drawReplayBoard(
                        StackPane boardArea,
                        JsonNode board,
                        String variantId) {

                boardArea.getChildren().clear();

                if (!board.isArray() || board.isEmpty()) {
                        boardArea.getChildren().add(
                                        new Label("No board data was saved."));
                        return;
                }

                // The API sends every square in one flat array.
                int boardSize = (int) Math.sqrt(board.size());
                double squareSize = 500.0 / boardSize;

                GridPane boardGrid = new GridPane();

                boardGrid.setPrefSize(500, 500);
                boardGrid.setMaxSize(500, 500);
                boardGrid.setAlignment(Pos.CENTER);

                for (int row = 0; row < boardSize; row++) {
                        for (int column = 0; column < boardSize; column++) {

                                int index = row * boardSize + column;
                                int pieceType = board.get(index).asInt(0);

                                StackPane square = new StackPane();

                                square.setPrefSize(squareSize, squareSize);
                                square.setMinSize(squareSize, squareSize);
                                square.setMaxSize(squareSize, squareSize);

                                boolean darkSquare = (row + column) % 2 == 1;

                                // Senegalese games use green and cream squares.
                                if ("SENEGALESE".equalsIgnoreCase(variantId)) {
                                        square.setStyle(
                                                        darkSquare
                                                                        ? "-fx-background-color: #00853F;"
                                                                        : "-fx-background-color: #FFF4C2;");
                                } else {
                                        square.setStyle(
                                                        darkSquare
                                                                        ? "-fx-background-color: #6B4423;"
                                                                        : "-fx-background-color: #F2D2A9;");
                                }

                                if (pieceType != 0) {
                                        square.getChildren().add(
                                                        createReplayPiece(
                                                                        pieceType,
                                                                        variantId,
                                                                        squareSize));
                                }

                                boardGrid.add(square, column, row);
                        }
                }

                boardArea.getChildren().add(boardGrid);
        }

        // Creates one regular piece or king for the replay board.
        private StackPane createReplayPiece(
                        int pieceType,
                        String variantId,
                        double squareSize) {

                boolean redPiece = pieceType == 1 || pieceType == 3;

                boolean king = pieceType == 3 || pieceType == 4;

                Color pieceColor;

                // Senegalese Checkers uses red and yellow pieces.
                if ("SENEGALESE".equalsIgnoreCase(variantId)) {
                        pieceColor = redPiece
                                        ? Color.web("#CE1126")
                                        : Color.web("#FCD116");
                } else {
                        pieceColor = redPiece
                                        ? Color.web("#C62828")
                                        : Color.web("#202020");
                }

                Circle piece = new Circle(squareSize * 0.34);
                piece.setFill(pieceColor);
                piece.setStroke(Color.web("#D4AF37"));
                piece.setStrokeWidth(3);

                StackPane pieceView = new StackPane(piece);

                // Places a K on pieces that were promoted to kings.
                if (king) {
                        Label kingLabel = new Label("K");

                        kingLabel.setStyle(
                                        "-fx-font-size: "
                                                        + Math.max(14, squareSize * 0.28)
                                                        + "px;"
                                                        + "-fx-font-weight: bold;"
                                                        + "-fx-text-fill: white;");

                        pieceView.getChildren().add(kingLabel);
                }

                return pieceView;
        }

        // Stores the information displayed in one table row.
        private record ReplayGame(
                        long gameId,
                        String date,
                        String opponent,
                        String variant,
                        String outcome) {
        }
}