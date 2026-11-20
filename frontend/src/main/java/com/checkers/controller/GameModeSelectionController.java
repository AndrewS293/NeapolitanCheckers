package com.checkers.controller;

import com.checkers.Main;
import com.checkers.localization.AppText;
import com.checkers.rules.GameRules;
import com.checkers.variants.VariantRegistry;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class GameModeSelectionController {

    private final Stage stage;
    private final String variantId;
    private final ComboBox<ReinforcementLearningTrainer.Difficulty> difficultyPicker =
            new ComboBox<>();
    private final RadioButton aiMode = new RadioButton(AppText.text("Play against AI"));
    private final Label modelStatus = new Label();
    private final Button startButton = new Button(AppText.text("Start Game"));

    public GameModeSelectionController(Stage stage, String variantId) {
        if (stage == null || variantId == null || variantId.isBlank()) {
            throw new IllegalArgumentException("Stage and variant are required.");
        }
        this.stage = stage;
        this.variantId = variantId;
    }

    public void show() {
        GameRules rules = VariantRegistry.defaults().get(variantId);
        Label title = new Label(AppText.text(rules.getDisplayName()));
        title.setStyle("-fx-font-family: Georgia; -fx-font-size: 25px;"
                + " -fx-text-fill: #173c36;");

        RadioButton localMode = new RadioButton(AppText.text("Local multiplayer"));
        localMode.setSelected(true);
        ToggleGroup modes = new ToggleGroup();
        localMode.setToggleGroup(modes);
        aiMode.setToggleGroup(modes);

        difficultyPicker.getItems().setAll(ReinforcementLearningTrainer.Difficulty.values());
        difficultyPicker.getSelectionModel().select(ReinforcementLearningTrainer.Difficulty.EASY);
        difficultyPicker.setMaxWidth(Double.MAX_VALUE);
        difficultyPicker.setDisable(true);
        localMode.setOnAction(event -> refreshModelAvailability());
        aiMode.setOnAction(event -> refreshModelAvailability());
        difficultyPicker.setOnAction(event -> refreshModelAvailability());
        refreshModelAvailability();

        startButton.setMaxWidth(Double.MAX_VALUE);
        AppTheme.stylePrimaryButton(startButton);
        startButton.setOnAction(event -> startGame());
        Button backButton = new Button(AppText.text("Back to variants"));
        AppTheme.styleSecondaryButton(backButton);
        backButton.setOnAction(event -> Main.showVariantSelection());

        VBox root = new VBox(18,
                title,
                new Label(AppText.text("Choose how you want to play.")),
                localMode,
                aiMode,
                new HBox(10, new Label(AppText.text("AI difficulty")), difficultyPicker),
                modelStatus,
                startButton,
                backButton);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(36));
        AppTheme.applyBackground(root);

        stage.setTitle(AppText.text("Neapolitan Checkers - Game Setup"));
        stage.setScene(new Scene(root, 520, 430));
        stage.setMinWidth(480);
        stage.setMinHeight(400);
        stage.show();
    }

    private void refreshModelAvailability() {
        boolean useAi = aiMode.isSelected();
        difficultyPicker.setDisable(!useAi);
        boolean available = !useAi || ReinforcementLearningTrainer.hasTrainedAgent(
                variantId, difficultyPicker.getValue());
        modelStatus.setText(useAi && !available
                ? AppText.text("No model is packaged for this variant and level. Train it first.")
                : useAi ? AppText.text("Trained model is available.") : "");
        startButton.setDisable(!available);
    }

    private void startGame() {
        if (!aiMode.isSelected()) {
            Main.showGameBoard(variantId, LocalGameOptions.human());
            return;
        }
        Main.showGameBoard(variantId, LocalGameOptions.ai(difficultyPicker.getValue()));
    }
}