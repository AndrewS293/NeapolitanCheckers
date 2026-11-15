package com.checkers.controller;

import com.checkers.Main;
import com.checkers.api.GameApi;
import com.checkers.model.User;
import com.checkers.session.SessionManager;
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

public class ReplayController {

    private final GameApi gameApi;

    // Stores the selected game's database ID.
    private long selectedGameId = -1;

    // Stores the API used to retrieve the player's saved games.
    public ReplayController(GameApi gameApi) {
        this.gameApi = gameApi;
    }

    // Creates the screen that displays the player's completed games.
    public Scene createScene() {

        Label title = new Label("Game History and Replays");
        title.setStyle(
                "-fx-font-size: 30px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: " + AppTheme.INK + ";");

        Label instructions = new Label(
                "Select a completed game to watch its replay.");
        instructions.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-text-fill: " + AppTheme.MUTED + ";");

        Label statusLabel = new Label("Loading your completed games...");
        statusLabel.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-text-fill: " + AppTheme.MUTED + ";");

        // Creates the table that displays the saved games.
        TableView<ReplayGame> historyTable = createHistoryTable();
        historyTable.setPlaceholder(
                new Label("No completed games were found."));

        // This button will open the replay viewer in the next step.
        Button watchReplayButton = new Button("Watch Selected Replay");
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
                                "Selected Game #" + selectedGameId);
                    }
                });

        watchReplayButton.setOnAction(event -> {
            statusLabel.setText(
                    "Opening replay for Game #" + selectedGameId + "...");

            // The replay board will be connected to this button next.
        });

        Button refreshButton = new Button("Refresh History");
        AppTheme.styleSecondaryButton(refreshButton);

        // Reloads the completed games from the database.
        refreshButton.setOnAction(event -> {
            selectedGameId = -1;
            watchReplayButton.setDisable(true);
            loadHistory(historyTable, statusLabel);
        });

        Button backButton = new Button("Back to Main Menu");
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

        TableColumn<ReplayGame, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                game.getValue().date()));

        TableColumn<ReplayGame, String> idColumn = new TableColumn<>("Game ID");
        idColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                String.valueOf(game.getValue().gameId())));

        TableColumn<ReplayGame, String> opponentColumn = new TableColumn<>("Opponent");
        opponentColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                game.getValue().opponent()));

        TableColumn<ReplayGame, String> variantColumn = new TableColumn<>("Variant");
        variantColumn.setCellValueFactory(game -> new ReadOnlyStringWrapper(
                game.getValue().variant()));

        TableColumn<ReplayGame, String> outcomeColumn = new TableColumn<>("Result");
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
                    "Your game history could not be loaded.");
            return;
        }

        historyTable.getItems().clear();
        historyTable.setDisable(true);
        statusLabel.setText("Loading your completed games...");

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
                                "Invalid server response (expected an array): "
                                        + preview);
                        return;
                    }

                    addGamesToTable(historyTable, history);
                    historyTable.setDisable(false);

                    if (historyTable.getItems().isEmpty()) {
                        statusLabel.setText(
                                "You do not have any completed games yet.");
                    } else {
                        statusLabel.setText(
                                "Select a game from the table.");
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
                        statusLabel.setText(
                                "Could not load your game history: "
                                        + reportedFailure);
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

            String variant = game.path("variant")
                    .asText("Unknown");

            String outcome = game.path("outcome")
                    .asText("Unknown");

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
            return "Unknown";
        }

        String readableDate = startedAt.replace("T", " ");

        if (readableDate.length() > 16) {
            readableDate = readableDate.substring(0, 16);
        }

        return readableDate;
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