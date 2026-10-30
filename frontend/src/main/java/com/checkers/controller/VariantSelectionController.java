
package com.checkers.controller;

import com.checkers.Main;
import com.checkers.variants.GameFactory;
import com.checkers.variants.VariantRegistry;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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
        title.setFont(Font.font("Georgia", 26));
        title.setStyle("-fx-text-fill: #173c36;");

        Label subtitle = new Label(
                "Select the ruleset you want to play.");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setStyle("-fx-text-fill: #52635d;");

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

        VBox turkishCard = createVariantCard(
                "Turkish Checkers",
                "------",
                "------",
                "TURKISH");
                
        VBox thaiCard = createVariantCard(
                "Thai Checkers",
                "-----",
                "-----",
                "THAI");

        VBox internationalCard = createVariantCard(
                "International Checkers",
                "-----",
                "-----",
                "INTERNATIONAL");

        VBox variantSixCard = createVariantCard(
                "Ndeye's Checkers",
                "-----",
                "-----",
                "VARIANTSIX");

        
                GridPane variants = new GridPane();
                variants.setAlignment(Pos.TOP_CENTER);
                variants.setHgap(14);
                variants.setVgap(14);

                for (int column = 0; column < 2; column++) {
                        ColumnConstraints constraints = new ColumnConstraints();
                        constraints.setPercentWidth(50);
                        constraints.setHgrow(Priority.ALWAYS);
                        constraints.setFillWidth(true);
                        variants.getColumnConstraints().add(constraints);
                }

                VBox[] cards = {
                        americanCard,
                        malaysianCard,
                        turkishCard,
                        thaiCard,
                        internationalCard,
                        variantSixCard
                };

                for (int index = 0; index < cards.length; index++) {
                        cards[index].setMaxWidth(Double.MAX_VALUE);
                        variants.add(cards[index], index % 2, index / 2);
                }

                ScrollPane options = new ScrollPane(variants);
                options.setFitToWidth(true);
                options.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                options.setStyle(
                                "-fx-background: transparent;"
                                + "-fx-background-color: transparent;"
                                + "-fx-padding: 2 4 2 2;");
                options.setMinHeight(0);

                VBox heading = new VBox(6, title, subtitle);
                heading.setAlignment(Pos.CENTER_LEFT);

                VBox root = new VBox(18, heading, options);
                root.setAlignment(Pos.TOP_LEFT);
                root.setPadding(new Insets(24));
                root.setStyle("-fx-background-color: #e9eee9;");
                VBox.setVgrow(options, Priority.ALWAYS);

                Scene scene = new Scene(root, 680, 720);
        stage.setTitle("Neapolitan Checkers - Select Variant");
        stage.setScene(scene);
                stage.setMinWidth(560);
                stage.setMinHeight(560);
        stage.show();
    }

    private VBox createVariantCard(
            String name,
            String boardInfo,
            String description,
            String variantId) {

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Georgia", 18));
        nameLabel.setStyle("-fx-text-fill: #173c36;");

        Label boardLabel = new Label(boardInfo);
        boardLabel.setFont(Font.font("Arial", 12));
        boardLabel.setWrapText(true);
        boardLabel.setStyle(
                "-fx-font-weight: bold;"
                + "-fx-text-fill: #a64b3c;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(260);
        descriptionLabel.setAlignment(Pos.CENTER);

        Button playButton = new Button("Play " + name);
        playButton.setMaxWidth(Double.MAX_VALUE);
        playButton.setPrefHeight(32);
        playButton.setStyle(
                "-fx-background-color: #245c50;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-background-radius: 5px;");

        playButton.setOnAction(event -> {
            // The factory creates GameLogic with the selected rules.
            Main.showGameBoard(variantId);
        });

        VBox card = new VBox(
                8, nameLabel, boardLabel, descriptionLabel, playButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: #ffffff;"
                + "-fx-background-radius: 6px;"
                + "-fx-border-color: #cbd6ce;"
                + "-fx-border-radius: 6px;");

        return card;
    }
}
