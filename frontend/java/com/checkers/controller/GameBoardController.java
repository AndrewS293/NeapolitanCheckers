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

public class GameBoardController {

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

        boolean moved =
                gameLogic.move(
                        selectedIndex,
                        clickedIndex
                );

        if (moved) {

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
}
