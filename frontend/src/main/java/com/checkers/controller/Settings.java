
package com.checkers.controller;

import com.checkers.Main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class Settings {

    // remembers the selected screen size
    private static String selectedScreenSize = "Medium";

    // creates the settings screen
    public Scene createScene(Runnable onResume, Runnable onLeave) {

        // screen title
        Label title = new Label("Settings");
        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + AppTheme.INK + ";"
        );

        // resume button
        Button resumeButton = new Button("Resume Game");
        AppTheme.stylePrimaryButton(resumeButton);
        resumeButton.setMaxWidth(220);
        resumeButton.setOnAction(event -> onResume.run());

        // screen size selection
        Label screenSizeLabel = new Label("Screen Size");
        screenSizeLabel.setStyle("-fx-font-size: 17px;");

        ComboBox<String> screenSize = new ComboBox<>();
        screenSize.getItems().addAll(
                "Small",
                "Medium",
                "Large",
                "Full Screen"
        );

        screenSize.setValue(selectedScreenSize);
        screenSize.setMaxWidth(220);

        screenSize.setOnAction(event -> {
            selectedScreenSize = screenSize.getValue();
            Main.setWindowSize(selectedScreenSize);
        });

        // language selection
        Label languageLabel = new Label("Language");
        languageLabel.setStyle("-fx-font-size: 17px;");

        ComboBox<String> language = new ComboBox<>();
        language.getItems().add("English");
        language.setValue("English");
        language.setMaxWidth(220);

        Label languageNote = new Label(
                "Additional languages coming soon."
        );
        languageNote.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + AppTheme.MUTED + ";"
        );

        // temporary volume setting
        Label volumeLabel = new Label("Volume");
        volumeLabel.setStyle("-fx-font-size: 17px;");

        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setMaxWidth(220);
        volumeSlider.setDisable(true);

        Label volumeNote = new Label("Sound settings coming soon.");
        volumeNote.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + AppTheme.MUTED + ";"
        );

        // leave game button
        Button leaveButton = new Button("Leave Game");
        AppTheme.styleSecondaryButton(leaveButton);
        leaveButton.setMaxWidth(220);

        leaveButton.setStyle(
                "-fx-background-color: " + AppTheme.ACCENT + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 5px;"
        );

        leaveButton.setOnAction(event -> {

            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION
            );

            confirmation.setTitle("Leave Game");
            confirmation.setHeaderText("Are you sure you want to leave?");
            confirmation.setContentText(
                    "You will return to the main menu."
            );

            Optional<ButtonType> result =
                    confirmation.showAndWait();

            if (result.isPresent()
                    && result.get() == ButtonType.OK) {
                onLeave.run();
            }
        });

        // organizes the settings controls vertically
        VBox root = new VBox(
                18,
                title,
                resumeButton,
                screenSizeLabel,
                screenSize,
                languageLabel,
                language,
                languageNote,
                volumeLabel,
                volumeSlider,
                volumeNote,
                leaveButton
        );

        // centers the content and adds padding
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(35));

        AppTheme.applyBackground(root);

        return new Scene(root, 700, 900);
    }
}
