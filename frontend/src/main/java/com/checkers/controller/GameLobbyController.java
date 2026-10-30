package com.checkers.controller;

import com.checkers.Main;
import com.checkers.api.GameApi;
import com.checkers.websocket.GameWebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.checkers.logic.GameLogic;
import com.checkers.variants.GameFactory;
import com.checkers.variants.VariantRegistry;

import javafx.scene.layout.GridPane;

import java.util.concurrent.CompletableFuture;

public class GameLobbyController {

    private final GameApi gameApi;

    private final Label statusLabel = new Label("Choose a multiplayer option.");
    private final Label roomLabel = new Label("No room selected.");

    private final ListView<String> gamesList = new ListView<>();
    private final TextField joinCodeField = new TextField();

    private String selectedVariantId = "AMERICAN";
    private String currentVariantId = "AMERICAN";

    private Button americanVariantButton;
    private Button malaysianVariantButton;

    private GameWebSocketClient webSocketClient;
    private String currentGameId;

    private int connectedPlayerNumber;

    private boolean gameBoardOpened = false;

    public GameLobbyController() {
        this.gameApi = new GameApi(Main.getAuthApi());
    }

    public Scene createScene() {
        Label title = new Label("Online Multiplayer");
        title.setStyle(
                "-fx-font-size: 28px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: white;"
        );

        Label variantTitle = new Label("Choose Your Variant");
        variantTitle.setStyle(
                "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: white;"
        );

        americanVariantButton = createVariantButton(
                "AMERICAN",
                "American Checkers",
                "8 × 8 Board",
                "Classic checkers with short kings.",
                true
        );

        malaysianVariantButton = createVariantButton(
                "MALAYSIAN",
                "Malaysian Draughts",
                "12 × 12 Board",
                "Flying kings, maximum captures and huffing.",
                true
        );

        Button thaiButton = createVariantButton(
                "THAI",
                "Thai Draughts",
                "8 × 8 Board",
                "----",
                false
        );

        Button turkishButton = createVariantButton(
                "TURKISH",
                "Turkish Draughts",
                "8 × 8 Board",
                "-----",
                false
        );

        updateVariantCardStyles();

        GridPane variantsGrid = new GridPane();
        variantsGrid.setHgap(12);
        variantsGrid.setVgap(12);
        variantsGrid.setAlignment(Pos.CENTER);

        variantsGrid.add(americanVariantButton, 0, 0);
        variantsGrid.add(malaysianVariantButton, 1, 0);
        variantsGrid.add(thaiButton, 0, 1);
        variantsGrid.add(turkishButton, 1, 1);

        Button createPublicButton = new Button("Create Public Game");
        createPublicButton.setOnAction(event ->
                createGame("PUBLIC"));

        Button createPrivateButton = new Button("Create Private Game");
        createPrivateButton.setOnAction(event ->
                createGame("PRIVATE"));

        Button refreshButton = new Button("Refresh Public Games");
        refreshButton.setOnAction(event ->
                refreshPublicGames());

        Button joinPublicButton = new Button("Join Selected Game");
        joinPublicButton.setOnAction(event ->
                joinSelectedPublicGame());

        joinCodeField.setPromptText("Enter private room join code");

        Button joinPrivateButton = new Button("Join Private Game");
        joinPrivateButton.setOnAction(event ->
                joinPrivateGame());

        Button backButton = new Button("Back to Menu");
        backButton.setOnAction(event -> {
            disconnectFromGame();
            Main.showMainMenu();
        });

        HBox createButtons = new HBox(
                10, createPublicButton, createPrivateButton
        );
        createButtons.setAlignment(Pos.CENTER);

        HBox publicGameButtons = new HBox(
                10, refreshButton, joinPublicButton
        );
        publicGameButtons.setAlignment(Pos.CENTER);

        HBox privateJoinRow = new HBox(
                10, joinCodeField, joinPrivateButton
        );
        privateJoinRow.setAlignment(Pos.CENTER);

        gamesList.setPrefHeight(150);
        gamesList.setPlaceholder(
                new Label("No public games loaded. Click Refresh.")
        );

        roomLabel.setWrapText(true);
        statusLabel.setWrapText(true);

        VBox root = new VBox(
                12,
                title,
                variantTitle,
                variantsGrid,
                new Label("Create a Game"),
                createButtons,
                new Label("Available Public Games"),
                gamesList,
                publicGameButtons,
                new Label("Join a Private Game"),
                privateJoinRow,
                roomLabel,
                statusLabel,
                backButton
        );

        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #1D222C;");

        return new Scene(root, 650, 950);
    }

    private void updateVariantCardStyles() {
        if (americanVariantButton != null) {
            americanVariantButton.setStyle(
                    variantButtonStyle(
                            selectedVariantId.equals("AMERICAN")
                    )
            );
        }

        if (malaysianVariantButton != null) {
            malaysianVariantButton.setStyle(
                    variantButtonStyle(
                            selectedVariantId.equals("MALAYSIAN")
                    )
            );
        }
    }

    private String variantButtonStyle(boolean selected) {
        return "-fx-background-color: #303541;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 12 16 12 16;"
                + "-fx-background-radius: 8;"
                + "-fx-border-radius: 8;"
                + "-fx-border-width: "
                + (selected ? "2" : "1") + ";"
                + "-fx-border-color: "
                + (selected ? "#D6B56D" : "#626A78") + ";";
    }

    private Button createVariantButton(
            String id,
            String name,
            String board,
            String description,
            boolean available) {

        Button button = new Button(
                board + "\n\n" + name + "\n\n"
                        + description + "\n\n"
                        + (available ? "AVAILABLE" : "COMING SOON")
        );

        button.setPrefSize(230, 155);
        button.setWrapText(true);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setDisable(!available);

        if (available) {
            button.setOnAction(event -> {
                selectedVariantId = id;
                updateVariantCardStyles();
            });
        }

        return button;
    }

    private void createGame(String visibility) {
        statusLabel.setText("Creating " + visibility.toLowerCase()
                + " game...");

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode response = gameApi.createGame(visibility, selectedVariantId);

                String gameId = getText(response, "gameId");
                String joinCode = getText(response, "joinCode");

                currentVariantId = selectedVariantId;

                if (gameId == null || gameId.isBlank()) {
                    throw new IllegalStateException(
                            "The server did not return a gameId. Response: "
                                    + response
                    );
                }

                Platform.runLater(() -> {
                    currentGameId = gameId;

                    String details = "Room ID: " + gameId
                            + "\nVisibility: " + visibility
                            + "\nVariant: " + currentVariantId
                            + (joinCode != null && !joinCode.isBlank()
                                ? "\nJoin Code: " + joinCode
                                : "")
                            + "\n\nConnecting to the game server...";

                    roomLabel.setText(details);
                    connectToGame(gameId);
                });

            } catch (Exception e) {
                System.err.println("========== CREATE GAME FAILED ==========");
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() ->
                    showError("Could not create game", e)
                );
            }
        });
    }

    private void refreshPublicGames() {
        statusLabel.setText("Loading public games...");

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode response = gameApi.getOpenGames();

                Platform.runLater(() -> {
                    gamesList.getItems().clear();

                    JsonNode games = response.isArray()
                            ? response
                            : response.has("games")
                                ? response.get("games")
                                : null;

                    if (games == null || !games.isArray()) {
                        statusLabel.setText(
                                "The server returned an unexpected public-games response."
                        );
                        return;
                    }

                    for (JsonNode game : games) {
                        String gameId = getText(game, "gameId");
                        String players = getText(game, "players");
                        String status = getText(game, "status");
                        String variantId = getText(game, "variantId");

                        if (gameId != null) {
                            gamesList.getItems().add(
                                    gameId
                                            + " | "
                                            + (variantId == null ? "Unknown" : variantId)
                                            + " | Players: "
                                            + (players == null ? "?" : players)
                                            + " | Status: "
                                            + (status == null ? "?" : status)
                            );
                        }
                    }

                    statusLabel.setText(
                            gamesList.getItems().isEmpty()
                                    ? "No public games are currently available."
                                    : "Select a public game to join."
                    );
                });

            } catch (Exception e) {
                showError("Could not load public games", e);
            }
        });
    }

    private void joinSelectedPublicGame() {
        String selected = gamesList.getSelectionModel()
                .getSelectedItem();

        if (selected == null || selected.isBlank()) {
            showAlert("Select a Game",
                    "Choose a public game from the list first.");
            return;
        }

        // The displayed format is: gameId | Players: ... | Status: ...
        String gameId = selected.split("\\s*\\|\\s*")[0].trim();

        statusLabel.setText("Joining public game...");

        CompletableFuture.runAsync(() -> {
            try {
                // REST validates the request. WebSocket joins the actual room.
                JsonNode response = gameApi.joinPublicGame(gameId);

                String variantId = response.path("variantId").asText("");

                if (variantId.isBlank()) {
                    throw new Exception("The server did not return the game's variant.");
                }

                currentVariantId = variantId;
                currentGameId = response.path("gameId").asText();

                Platform.runLater(() -> {
                    currentGameId = gameId;
                    currentVariantId = variantId;

                    roomLabel.setText(
                            "Room ID: " + gameId
                                    + "\nVariant: " + currentVariantId
                                    + "\nConnecting to the game server..."
                    );

                    connectToGame(gameId);
                });

            } catch (Exception e) {
                showError("Could not join public game", e);
            }
        });
    }

    private void joinPrivateGame() {
        String joinCode = joinCodeField.getText().trim();

        if (joinCode.isBlank()) {
            showAlert("Missing Join Code",
                    "Enter the private game's join code.");
            return;
        }

        statusLabel.setText("Joining private game...");

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode response = gameApi.joinPrivateGame(joinCode);

                String gameId = getText(response, "gameId");

                String variantId = getText(response, "variantId");

                if (variantId == null || variantId.isBlank()) {
                    throw new IllegalStateException(
                            "The server did not return the game's variant."
                    );
                }

                if (gameId == null || gameId.isBlank()) {
                    throw new IllegalStateException(
                            "The server did not return a gameId. Response: "
                                    + response
                    );
                }

                Platform.runLater(() -> {
                    currentGameId = gameId;
                    currentVariantId = variantId;

                    roomLabel.setText(
                            "Room ID: " + gameId
                                    + "\nVariant: " + currentVariantId
                                    + "\nConnecting to the game server..."
                    );

                    connectToGame(gameId);
                });

            } catch (Exception e) {
                showError("Could not join private game", e);
            }
        });
    }

    private void connectToGame(String gameId) {
        statusLabel.setText("Connecting to room " + gameId + "...");

        // Close an earlier connection if the player changes rooms.
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }

        webSocketClient = new GameWebSocketClient(
                Main.getAuthApi().getClient(),
                message -> handleServerMessage(message),
                status -> Platform.runLater(() ->
                        statusLabel.setText(status))
        );

        webSocketClient.connect(gameId, "");
    }

    private void handleServerMessage(JsonNode message) {
        String type = getText(message, "type");
        String gameId = getText(message, "gameId");
        String username = getText(message, "username");
        String serverMessage = getText(message, "message");
        String players = getText(message, "players");
        String status = getText(message, "status");
        String player = getText(message, "player");

        Platform.runLater(() -> {
            if (type == null) {
                statusLabel.setText("Received a server message without a type.");
                return;
            }

            switch (type) {
                case "CONNECTED":
                    connectedPlayerNumber = message.path("player").asInt(0);

                    System.out.println(
                            "CONNECTED received: player="
                                    + connectedPlayerNumber
                                    + ", gameId="
                                    + message.path("gameId").asText()
                                    + ", message="
                                    + message
                    );

                    Platform.runLater(() ->
                            statusLabel.setText(
                                    "Connected. Player " + connectedPlayerNumber
                                            + ", players: "
                                            + message.path("players").asInt()
                            )
                    );
                    break;


                case "PLAYER_JOINED":
                    statusLabel.setText(
                            (username == null ? "A player" : username)
                                    + " joined. Players: "
                                    + (players == null ? "?" : players)
                    );
                    break;


                case "PLAYER_LEFT":
                    statusLabel.setText(
                            (username == null ? "A player" : username)
                                    + " left the room."
                    );
                    break;

                case "GAME_WAITING":
                    statusLabel.setText(
                            "Waiting for another player..."
                    );
                    break;

                
                
                case "GAME_STARTED":
                    System.out.println("Game started.");
                    System.out.println(
                            "gameBoardOpened=" + gameBoardOpened
                                    + ", gameId=" + currentGameId
                                    + ", player=" + connectedPlayerNumber
                    );
                    if (gameBoardOpened || webSocketClient == null
                            || currentGameId == null || connectedPlayerNumber == 0) {
                        break;
                    }

                    gameBoardOpened = true;

                    GameFactory factory = new GameFactory(
                            VariantRegistry.defaults()
                    );

                    GameLogic onlineGame = factory.createGame(currentVariantId);

                    GameBoardController boardController =
                            new GameBoardController(onlineGame);

                    boardController.configureOnlineGame(
                            currentGameId,
                            connectedPlayerNumber,
                            webSocketClient
                    );

                    // Transfer future messages to this same board controller.
                    webSocketClient.setMessageHandler(
                            boardController::handleServerMessage
                    );

                    Platform.runLater(() ->
                            Main.showOnlineGameBoard(boardController)
                    );
                    break;



                default:
                    statusLabel.setText(
                            type
                                    + (serverMessage == null
                                        ? ""
                                        : ": " + serverMessage)
                    );
                    break;
            }
        });
    }

    private void disconnectFromGame() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
            webSocketClient = null;
        }

        currentGameId = null;
    }

    private String getText(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }

        JsonNode value = node.get(field);
        return value.isValueNode() ? value.asText() : value.toString();
    }

    private void showError(String title, Exception e) {
        Platform.runLater(() -> {
            statusLabel.setText(title + ": " + e.getMessage());

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
