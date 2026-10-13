package com.checkers.controller;

import com.checkers.Main;
import com.checkers.logic.Board;
import com.checkers.logic.GameLogic;
import com.checkers.logic.Piece;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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


import com.checkers.websocket.GameWebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;




public class GameBoardController {
    private boolean onlineMode = false;
    private boolean awaitingServerMove = false;
    private String onlineGameId;
    private int onlinePlayerNumber;
    private GameWebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private static final int BOARD_SIZE = 8;
    private static final double SQUARE_SIZE = 65;

    private GameLogic gameLogic;
    private BorderPane root;
    private GridPane board;
    private Label gameStatus;
    private int selectedIndex = -1;


    public Scene createScene() {

        gameLogic = new GameLogic();

        Label title = new Label("Neapolitan Checkers");
        title.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;"
        );

        gameStatus = new Label();
        gameStatus.setStyle("-fx-font-size: 16px");

        updateGameStatus();

        board = createBoard();
        board.setAlignment(Pos.CENTER);

        Button newGameButton = new Button("New Game");
        Button backButton = new Button("Back to Menu");

        newGameButton.setOnAction(event -> {
            gameLogic = new GameLogic();
            selectedIndex = -1;
            refreshBoard();
            updateGameStatus();
        });

        backButton.setOnAction(event -> {
            Main.showMainMenu();
        });

        HBox buttons = new HBox(
                15,
                newGameButton,
                backButton
        );

        buttons.setAlignment(Pos.CENTER);

        VBox topSection = new VBox(
                10,
                title,
                gameStatus
        );

        topSection.setAlignment(Pos.CENTER);

        root = new BorderPane();

        root.setPadding(new Insets(25));
        root.setTop(topSection);
        root.setCenter(board);
        root.setBottom(buttons);

        BorderPane.setMargin(
                board,
                new Insets(20, 0, 20, 0)
        );

        return new Scene(root);
    }

    private GridPane createBoard() {

        GridPane board = new GridPane();

        for (int row = 0; row < BOARD_SIZE; row++) {

            for (int column = 0;
                 column < BOARD_SIZE;
                 column++) {

                StackPane square =
                        createSquare(row, column);

                board.add(square, column, row);
            }
        }

        return board;
    }

    private StackPane createSquare(
            int row,
            int column) {

        StackPane square = new StackPane();

        Rectangle background =
                new Rectangle(
                        SQUARE_SIZE,
                        SQUARE_SIZE
                );

        boolean darkSquare =
                (row + column) % 2 != 0;

        if (darkSquare) {
            background.setFill(
                    Color.SADDLEBROWN //we can make a variable here for board colors
            );
        } else {
            background.setFill(
                    Color.BISQUE
            );
        }

        square.getChildren().add(background);

        int index = Board.toIndex(row, column);

        if (index != -1) {
            Piece piece = gameLogic.getBoard().getPiece(index);

            if (piece !=null && !piece.isEmpty()) {
                Color pieceColor =
                        piece.isBlack()
                                ? Color.BLACK //again we can have variables for colors
                                : Color.FIREBRICK;

                Circle pieceCircle =
                        createPiece(piece);

                square.getChildren().add(pieceCircle);
            }

            final int finalIndex = index;

            square.setOnMouseClicked(event -> {
                handleSquareClick(finalIndex);
            });
        }

        return square;
    }

    private Circle createPiece(Piece piece) {

        Color color;

        if (piece.isBlack()) {
            color = Color.BLACK;
        } else if (piece.isRed()) {
            color = Color.FIREBRICK;
        } else {
            return null; 
        }

        Circle circle = new Circle(23);

        circle.setFill(color);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);


        if(piece.isKing()) {
            circle.setStroke(Color.GOLD);
            circle.setStrokeWidth(4);
        }

        return circle;
    }

    private void handleSquareClick(int clickedIndex) {

        if (selectedIndex == -1) {

            
            if (onlineMode && !isLocalPlayersPiece(clickedIndex)) {
                System.out.println("Cannot select opponent's piece. "
                        + "Online player: " + onlinePlayerNumber);
                return;
            }


            if (gameLogic.isCurrentPlayerPiece(clickedIndex)) {

                selectedIndex = clickedIndex;

                gameStatus.setText(
                        "Piece selected. Choose a destination."
                );

            } else {

                gameStatus.setText(
                        "Select one of your pieces."
                );
            }

            return;
        }

        
        if (selectedIndex != -1 && selectedIndex != clickedIndex) {
            
            if (onlineMode) {
                int fromRow = Board.getRow(selectedIndex);
                int fromCol = Board.getCol(selectedIndex);
                int toRow = Board.getRow(clickedIndex);
                int toCol = Board.getCol(clickedIndex);

                System.out.println(
                    "Destination selected: from=" + selectedIndex
                        + ", to=" + clickedIndex
                        + ", player=" + onlinePlayerNumber
                        + ", awaitingServerMove=" + awaitingServerMove
                );

                requestMove(fromRow, fromCol, toRow, toCol);
                return;
            }


            // Preserve existing local-game behavior.
            if (gameLogic.move(selectedIndex, clickedIndex)) {
                selectedIndex = -1;
                refreshBoard();
                updateGameStatus();
            } else {
                if (gameLogic.isCurrentPlayerPiece(clickedIndex)) {

                    selectedIndex = clickedIndex;

                    gameStatus.setText(
                            "Piece selected. Choose a destination."
                    );

                } else {

                    gameStatus.setText(
                            "Invalid move. Try again."
                    );
                }
            }

            return;
        }

    }

    private void refreshBoard() {

        GridPane newBoard = createBoard();

        newBoard.setAlignment(Pos.CENTER);

        board = newBoard;

        root.setCenter(board);
    }

    private void updateGameStatus() {

        if (gameLogic.getCurrentPlayer()
                == GameLogic.red_player) {

            gameStatus.setText(
                    "Red player's turn"
            );

        } else {

            gameStatus.setText(
                    "Black player's turn"
            );
        }
    }

        
    public void configureOnlineGame(
            String gameId,
            int playerNumber,
            GameWebSocketClient client) {

        this.onlineMode = true;
        this.onlineGameId = gameId;
        this.onlinePlayerNumber = playerNumber;
        this.webSocketClient = client;
    }


    
    private boolean isLocalPlayersPiece(int index) {
        Piece piece = gameLogic.getBoard().getPiece(index);

        if (piece == null) {
            return false;
        }

        if (onlinePlayerNumber == 1) {
            return piece.isRed();
        } else if (onlinePlayerNumber == 2) {
            return piece.isBlack();
        }

        return false;
    }

    
    private int messagePlayerFromOuter(JsonNode outerMessage, int fallback) {
        return outerMessage.path("player").asInt(fallback);
    }

    
    
    public void handleServerMessage(JsonNode message) {
        try {
            String type = message.path("type").asText("");

            // The server wraps the original MOVE message inside GAME_MESSAGE.
                    
        if ("GAME_MESSAGE".equals(type)) {
            int senderPlayer = message.path("player").asInt(-1);
            JsonNode payload = message.path("message");

            if (payload.isTextual()) {
                message = objectMapper.readTree(payload.asText());
            } else if (payload.isObject()) {
                message = payload;
            } else {
                System.err.println("GAME_MESSAGE has an invalid payload: "
                        + payload);
                return;
            }

            if (message.isObject()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) message)
                        .put("player", senderPlayer);
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
                }

                default ->
                    System.out.println("Unhandled server message: " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle server message: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }


    
    private void handleOnlineMove(JsonNode message) {
        int movePlayer = message.path("player").asInt(-1);

        System.out.println("Received MOVE from player " + movePlayer
                + "; local player is " + onlinePlayerNumber);

        Platform.runLater(() -> {
            try {
                // The server may send the move payload as a JSON string.
                JsonNode moveData = message.path("message");

                if (moveData.isTextual()) {
                    moveData = objectMapper.readTree(moveData.asText());
                }

                int fromRow = moveData.path("fromRow").asInt(-1);
                int fromCol = moveData.path("fromCol").asInt(-1);
                int toRow = moveData.path("toRow").asInt(-1);
                int toCol = moveData.path("toCol").asInt(-1);

                if (fromRow < 0 || fromRow >= 8
                        || fromCol < 0 || fromCol >= 8
                        || toRow < 0 || toRow >= 8
                        || toCol < 0 || toCol >= 8) {
                    System.err.println("Invalid move coordinates received.");
                    return;
                }

                boolean isOwnMove = movePlayer == onlinePlayerNumber;

                System.out.println(isOwnMove
                        ? "Processing our own echoed move."
                        : "Processing opponent's move.");

                int start = Board.toIndex(fromRow, fromCol);
                int end = Board.toIndex(toRow, toCol);

                if (start == -1 || end == -1) {
                    System.err.println("Server sent invalid or unplayable coordinates.");
                    return;
                }

                boolean moved = gameLogic.move(start, end);

                if (moved) {
                    selectedIndex = -1;
                    refreshBoard();
                    updateGameStatus();

                    System.out.println("Online move applied successfully.");
                } else {
                    System.err.println("Online move could not be applied.");
                }

            } catch (Exception e) {
                System.err.println("Error processing online move: "
                        + e.getMessage());
                e.printStackTrace();
            } finally {
                // Unlock only after our own move has been acknowledged.
                if (movePlayer == onlinePlayerNumber) {
                    awaitingServerMove = false;
                    System.out.println("Own move acknowledged; input unlocked.");
                }
            }
        });
    }


    private boolean validCoordinate(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }


    
    private void requestMove(
            int fromRow,
            int fromCol,
            int toRow,
            int toCol) {

        if (!onlineMode) {
            // Keep your existing local-game move implementation here.
            int start = fromRow * 8 + fromCol;
            int end = toRow * 8 + toCol;

            boolean moved = gameLogic.move(start, end);

            if (moved) {
                selectedIndex = -1;
                refreshBoard();
                updateGameStatus();
            }
            return;
        }

        
        if (awaitingServerMove) {
            System.out.println("Move not sent: waiting for server response.");
            return;
        }

        if (webSocketClient == null || onlineGameId == null) {
            System.out.println("Move not sent: WebSocket client or game ID is missing.");
            return;
        }

        awaitingServerMove = true;

        System.out.println(
            "Sending online move: " + fromRow + "," + fromCol
                + " -> " + toRow + "," + toCol
        );

        webSocketClient.sendMove(
            onlineGameId,
            fromRow,
            fromCol,
            toRow,
            toCol
        );

        selectedIndex = -1;
        refreshBoard();
    }
}