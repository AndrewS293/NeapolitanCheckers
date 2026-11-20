
package com.checkers.controller;

import com.checkers.Main;
import com.checkers.localization.AppLanguage;
import com.checkers.localization.AppText;

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
    public Scene createScene(Runnable onResume, Runnable onLeave,
                            Runnable onLanguageChanged) {

        // screen title
        Label title = new Label(AppText.text("Settings"));
        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + AppTheme.INK + ";"
        );

        // resume button
        Button resumeButton = new Button(AppText.text("Resume Game"));
        AppTheme.stylePrimaryButton(resumeButton);
        resumeButton.setMaxWidth(220);
        resumeButton.setOnAction(event -> onResume.run());

        // screen size selection
        Label screenSizeLabel = new Label(AppText.text("Screen Size"));
        screenSizeLabel.setStyle("-fx-font-size: 17px;");

        ComboBox<String> screenSize = new ComboBox<>();
        screenSize.getItems().addAll(
                AppText.text("Small"),
                AppText.text("Medium"),
                AppText.text("Large"),
                AppText.text("Full Screen")
        );

        screenSize.setValue(AppText.text(selectedScreenSize));
        screenSize.setMaxWidth(220);

        screenSize.setOnAction(event -> {
            String selected = screenSize.getValue();
            if (AppText.text("Small").equals(selected)) {
                selectedScreenSize = "Small";
            } else if (AppText.text("Medium").equals(selected)) {
                selectedScreenSize = "Medium";
            } else if (AppText.text("Large").equals(selected)) {
                selectedScreenSize = "Large";
            } else if (AppText.text("Full Screen").equals(selected)) {
                selectedScreenSize = "Full Screen";
            } else {
                selectedScreenSize = selected;
            }
            Main.setWindowSize(selectedScreenSize);
        });

        // language selection
        Label languageLabel = new Label(AppText.text("Language"));
        languageLabel.setStyle("-fx-font-size: 17px;");

        ComboBox<AppLanguage> language = AppText.languagePicker(onLanguageChanged);
        language.setMaxWidth(220);

        Label languageNote = new Label(AppText.text(
                "Changing language will update the app interface."));
        languageNote.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + AppTheme.MUTED + ";"
        );

        // temporary volume setting
        Label volumeLabel = new Label(AppText.text("Volume"));
        volumeLabel.setStyle("-fx-font-size: 17px;");

        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setMaxWidth(220);
        volumeSlider.setDisable(true);

        Label volumeNote = new Label(AppText.text("Sound settings coming soon."));
        volumeNote.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + AppTheme.MUTED + ";"
        );

        // leave game button
        Button leaveButton = new Button(AppText.text("Leave Game"));
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

            confirmation.setTitle(AppText.text("Leave Game"));
            confirmation.setHeaderText(AppText.text("Are you sure you want to leave?"));
            confirmation.setContentText(AppText.text(
                    "You will return to the main menu."));

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
