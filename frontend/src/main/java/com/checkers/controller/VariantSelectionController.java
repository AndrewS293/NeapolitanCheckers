
package com.checkers.controller;

import com.checkers.Main;
import com.checkers.variants.GameFactory;
import com.checkers.variants.VariantRegistry;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class VariantSelectionController {

    private final Stage stage;
    private final GameFactory gameFactory;

    public VariantSelectionController(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null.");
        }

        this.stage = stage;
        this.gameFactory = new GameFactory(VariantRegistry.defaults());
    }

    public void show() {
        Label title = new Label("Choose Your Checkers Variant");
        title.setFont(Font.font("Arial", 26));

        Label subtitle = new Label(
                "Select the ruleset you want to play.");
        subtitle.setFont(Font.font("Arial", 14));

        VBox americanCard = createVariantCard(
                "American Checkers",
                "8 × 8 board  •  12 pieces per player",
                "Forward-moving men, short-range kings, "
                        + "mandatory captures.",
                "AMERICAN");

        VBox malaysianCard = createVariantCard(
                "Malaysian Checkers",
                "12 × 12 board  •  30 pieces per player",
                "Flying kings, maximum captures, "
                        + "deferred capture removal, and huffing.",
                "MALAYSIAN");

        Label note = new Label(
                "Malaysian rules use the implementation currently "
                        + "configured in this project.");
        note.setWrapText(true);
        note.setMaxWidth(420);
        note.setStyle("-fx-text-fill: #666666;");

        VBox root = new VBox(
                20, title, subtitle, americanCard, malaysianCard, note);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f4f1e9;");

        Scene scene = new Scene(root, 650, 720);
        stage.setTitle("Neapolitan Checkers - Select Variant");
        stage.setScene(scene);
        stage.setMinWidth(550);
        stage.setMinHeight(650);
        stage.show();
    }

    private VBox createVariantCard(
            String name,
            String boardInfo,
            String description,
            String variantId) {

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", 20));

        Label boardLabel = new Label(boardInfo);
        boardLabel.setFont(Font.font("Arial", 14));
        boardLabel.setStyle("-fx-font-weight: bold;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(400);
        descriptionLabel.setAlignment(Pos.CENTER);

        Button playButton = new Button("Play " + name);
        playButton.setPrefWidth(200);
        playButton.setPrefHeight(38);
        playButton.setStyle(
                "-fx-background-color: #394b35;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-background-radius: 6px;");

        playButton.setOnAction(event -> {
            // The factory creates GameLogic with the selected rules.
            Main.showGameBoard(variantId);
        });

        VBox card = new VBox(
                12, nameLabel, boardLabel, descriptionLabel, playButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22));
        card.setMaxWidth(480);
        card.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 10px;"
                + "-fx-border-color: #d5d0c5;"
                + "-fx-border-radius: 10px;");

        return card;
    }
}
