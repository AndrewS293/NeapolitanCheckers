package com.checkers.controller;

import com.checkers.Main;

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

    public Scene createScene() {

        Label title = new Label("Neapolitan Checkers");
        title.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;"
        );

        Label gameStatus = new Label("Red player's turn");
        gameStatus.setStyle("-fx-font-size: 16px;");

        GridPane board = createBoard();
        board.setAlignment(Pos.CENTER);

        Button newGameButton = new Button("New Game");
        Button backButton = new Button("Back to Menu");

        newGameButton.setOnAction(event -> {
            gameStatus.setText("A new game has started!");
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

        BorderPane root = new BorderPane();

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
                    Color.SADDLEBROWN
            );
        } else {
            background.setFill(
                    Color.BISQUE
            );
        }

        square.getChildren().add(background);

        if (darkSquare && row < 3) {
            Circle blackPiece = createPiece(
                    Color.BLACK
            );

            square.getChildren().add(blackPiece);
        }

        if (darkSquare && row > 4) {
            Circle redPiece = createPiece(
                    Color.FIREBRICK
            );

            square.getChildren().add(redPiece);
        }

        return square;
    }

    private Circle createPiece(Color color) {

        Circle piece = new Circle(23);

        piece.setFill(color);
        piece.setStroke(Color.WHITE);
        piece.setStrokeWidth(2);

        return piece;
    }
}
