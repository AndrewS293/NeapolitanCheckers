
package com.checkers.controller;

import com.checkers.Main;
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
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.Collections;
import java.util.List;

public class GameBoardController {

    private boolean onlineMode = false;
    private boolean awaitingServerMove = false;

    private String onlineGameId;
    private int onlinePlayerNumber;
    private GameWebSocketClient webSocketClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double SQUARE_SIZE = GameStyleConfig.SQUARE_SIZE;

    private GameLogic gameLogic;
    private BorderPane root;
    private GridPane board;
    private Label gameStatus;

    private int selectedIndex = -1;

    private List<Integer> legalDestinations = Collections.emptyList();

    private boolean gameOverMessageShown = false;

    public GameBoardController() {
        this(new GameLogic());
    }

    public GameBoardController(GameLogic gameLogic) {
        if (gameLogic == null) {
            throw new IllegalArgumentException(
                    "GameLogic cannot be null.");
        }

        this.gameLogic = gameLogic;
    }

    public Scene createScene() {
        onlineMode = false;
        awaitingServerMove = false;

        Label title = new Label("Neapolitan Checkers");
        title.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;"
        );

        gameStatus = new Label();
        gameStatus.setStyle("-fx-font-size: 16px;");

        board = createBoard();
        board.setAlignment(Pos.CENTER);

        updateGameStatus();

        Button newGameButton = new Button("New Game");
        Button backButton = new Button("Back to Menu");

        newGameButton.setOnAction(event -> {
            gameLogic = new GameLogic(gameLogic.getRules());

            selectedIndex = -1;
            legalDestinations = Collections.emptyList();
            gameOverMessageShown = false;
            awaitingServerMove = false;

            refreshBoard();
            updateGameStatus();
        });

        backButton.setOnAction(event -> Main.showMainMenu());

        HBox buttons = new HBox(15, newGameButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        VBox topSection = new VBox(10, title, gameStatus);
        topSection.setAlignment(Pos.CENTER);

        root = new BorderPane();
        root.setPadding(new Insets(25));
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

        square.setMinSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setMaxSize(SQUARE_SIZE, SQUARE_SIZE);

        Rectangle background = new Rectangle(
                SQUARE_SIZE,
                SQUARE_SIZE
        );

        background.setStrokeType(
                javafx.scene.shape.StrokeType.INSIDE
        );


        boolean darkSquare = getCurrentBoard()
                .getConfig()
                .isPlayableSquare(row, col);

        background.setFill(
                darkSquare
                        ? GameStyleConfig.DARK_SQUARE_COLOR
                        : GameStyleConfig.LIGHT_SQUARE_COLOR
        );

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

        if (piece.isBlack()) {
            color = GameStyleConfig.BLACK_PIECE_COLOR;
        } else if (piece.isRed()) {
            color = GameStyleConfig.RED_PIECE_COLOR;
        } else {
            return null;
        }

        Circle circle = new Circle(SQUARE_SIZE * 0.35);

        circle.setStrokeType(
                javafx.scene.shape.StrokeType.INSIDE
        );

        circle.setFill(color);
        circle.setStroke(GameStyleConfig.PIECE_OUTLINE_COLOR);
        circle.setStrokeWidth(2);

        if (piece.isKing()) {
            circle.setStroke(Color.GOLD);
            circle.setStrokeWidth(4);
        }

        return circle;
    }

    private void handleSquareClick(int clickedIndex) {
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
                List<Integer> destinations =
                        gameLogic.getLegalDestinations(clickedIndex);

                if (destinations.isEmpty()) {
                    gameStatus.setText(
                            gameLogic.hasAnyJump(gameLogic.getCurrentPlayer())
                                    ? "A jump is available. Select a piece that can jump."
                                    : "That piece has no legal moves."
                    );
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
                    List<Integer> destinations =
                            gameLogic.getLegalDestinations(clickedIndex);

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

        if (gameLogic.move(selectedIndex, clickedIndex)) {
            updateSelectionAfterMove();
            refreshBoard();
            updateGameStatus();
            showGameOverMessage();
            return;
        }

        if (gameLogic.isCurrentPlayerPiece(clickedIndex)) {
            List<Integer> destinations =
                    gameLogic.getLegalDestinations(clickedIndex);

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

    private void updateSelectionAfterMove() {
        if (gameLogic.isMultiJumpInProgress()) {
            selectedIndex = gameLogic.getForcedJumpIndex();
            legalDestinations =
                    gameLogic.getLegalDestinations(selectedIndex);
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

    private void showGameOverMessage() {
        if (!gameLogic.isGameOver() || gameOverMessageShown) {
            return;
        }

        gameOverMessageShown = true;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(gameLogic.getGameResultMessage());
        alert.setContentText("Select New Game to play again.");
        alert.show();
    }

    public void configureOnlineGame(
            String gameId,
            int playerNumber,
            GameWebSocketClient client
    ) {
        onlineMode = true;
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
                            "GAME_MESSAGE has an invalid payload: " + payload
                    );
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

                case "PLAYER_JOINED" ->
                        System.out.println("Player joined: " + message);

                case "MOVE" ->
                        handleOnlineMove(message);

                case "PLAYER_LEFT" ->
                        System.out.println("Player left: " + message);

                case "ERROR" -> {
                    awaitingServerMove = false;
                    System.err.println("Server error: " + message);
                    Platform.runLater(() ->
                            gameStatus.setText("Server rejected the move.")
                    );
                }

                default ->
                        System.out.println("Unhandled server message: " + message);
            }
        } catch (Exception e) {
            System.err.println(
                    "Failed to handle server message: " + e.getMessage()
            );
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
                            "Server sent invalid or unplayable coordinates."
                    );
                    return;
                }

                boolean moved = gameLogic.move(start, end);

                if (moved) {
                    selectedIndex = -1;
                    legalDestinations = Collections.emptyList();

                    refreshBoard();
                    updateGameStatus();
                    showGameOverMessage();

                    System.out.println("Online move applied successfully.");
                } else {
                    System.err.println(
                            "Online move could not be applied locally."
                    );
                }
            } catch (Exception e) {
                System.err.println(
                        "Error processing online move: " + e.getMessage()
                );
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
            int toCol
    ) {
        if (!onlineMode) {
            Board currentBoard = getCurrentBoard();

            int start = currentBoard.indexAt(fromRow, fromCol);
            int end = currentBoard.indexAt(toRow, toCol);

            if (start >= 0 && end >= 0
                    && gameLogic.move(start, end)) {
                updateSelectionAfterMove();
                refreshBoard();
                updateGameStatus();
                showGameOverMessage();
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
                toCol
        );

        selectedIndex = -1;
        legalDestinations = Collections.emptyList();
        refreshBoard();
    }
}
