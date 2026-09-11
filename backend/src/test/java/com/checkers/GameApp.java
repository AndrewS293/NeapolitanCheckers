package com.checkers;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class GameApp extends Application {

    private static final double BASE_WIDTH = 1920;
    private static final double BASE_HEIGHT = 1080;

    private Stage window;
    private Scene scene;
    private Group scaleGroup;
    private StackPane gameUI;

    private final GameSettings settings = new GameSettings();

    private ComboBox<String> resolutionComboBox;
    private CheckBox fullscreenCheckBox;
    private CheckBox soundToggle;
    private Slider volumeSlider;
    private Label volumeValueLabel;

    private VBox menuLayout;
    private VBox optionsLayout;

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("Neapolitan Checkers");

        buildUi();

        scene = new Scene(gameUI, BASE_WIDTH / 2, BASE_HEIGHT / 2);
        scaleGroup.scaleXProperty().bind(
            Bindings.createDoubleBinding(() -> {
                double scaleX = scene.getWidth() / BASE_WIDTH;
                double scaleY = scene.getHeight() / BASE_HEIGHT;
                return Math.min(scaleX, scaleY);
            }, scene.widthProperty(), scene.heightProperty())
        );
        scaleGroup.scaleYProperty().bind(scaleGroup.scaleXProperty());

        window.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            settings.setFullscreen(newVal);
            refreshOptionsControls();
        });

        window.setScene(scene);
        window.setWidth(settings.getWidth());
        window.setHeight(settings.getHeight());
        window.show();
    }

    private void buildUi() {
        gameUI = new StackPane();
        gameUI.setStyle("-fx-background-color: black;");

        scaleGroup = new Group();

        StackPane contentRoot = new StackPane();
        contentRoot.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        contentRoot.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        contentRoot.setMaxSize(BASE_WIDTH, BASE_HEIGHT);
        contentRoot.setStyle("-fx-background-color: #0d0d1a;");

        menuLayout = createMenuLayout();
        optionsLayout = createOptionsLayout();

        menuLayout.setVisible(true);
        optionsLayout.setVisible(false);
        contentRoot.getChildren().addAll(menuLayout, optionsLayout);

        scaleGroup.getChildren().add(contentRoot);
        gameUI.getChildren().add(scaleGroup);
    }

    private VBox createMenuLayout() {
        Label titleLabel = new Label("Neapolitan Checkers");
        titleLabel.setStyle("-fx-font-size: 80px; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        Button startButton = new Button("PLAY");
        Button optionsButton = new Button("OPTIONS");
        Button exitButton = new Button("EXIT");

        String buttonStyle = "-fx-background-color: #222; -fx-text-fill: #00ff00; -fx-font-size: 34px; " +
                             "-fx-pref-width: 360px; -fx-pref-height: 72px; -fx-border-color: #00ff00; -fx-border-width: 2px; -fx-cursor: hand;";

        startButton.setStyle(buttonStyle);
        optionsButton.setStyle(buttonStyle);
        exitButton.setStyle(buttonStyle);

        startButton.setOnMouseEntered(e -> startButton.setStyle(buttonStyle + "-fx-background-color: #00ff00; -fx-text-fill: #222;"));
        startButton.setOnMouseExited(e -> startButton.setStyle(buttonStyle));
        optionsButton.setOnMouseEntered(e -> optionsButton.setStyle(buttonStyle + "-fx-background-color: #00ff00; -fx-text-fill: #222;"));
        optionsButton.setOnMouseExited(e -> optionsButton.setStyle(buttonStyle));
        exitButton.setOnMouseEntered(e -> exitButton.setStyle(buttonStyle + "-fx-background-color: #00ff00; -fx-text-fill: #222;"));
        exitButton.setOnMouseExited(e -> exitButton.setStyle(buttonStyle));

        startButton.setOnAction(e -> showGameScreen());
        optionsButton.setOnAction(e -> showOptionsScreen());
        exitButton.setOnAction(e -> window.close());

        VBox layout = new VBox(28);
        layout.setAlignment(Pos.CENTER);
        layout.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        layout.getChildren().addAll(titleLabel, startButton, optionsButton, exitButton);
        return layout;
    }

    private VBox createOptionsLayout() {
        Label titleLabel = new Label("Options");
        titleLabel.setStyle("-fx-font-size: 68px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label resolutionLabel = new Label("Resolution:");
        resolutionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");
        resolutionComboBox = new ComboBox<>();
        resolutionComboBox.getItems().addAll("800x600", "1024x768", "1280x720", "1366x768", "1920x1080");
        resolutionComboBox.setValue(settings.getResolution());
        resolutionComboBox.setOnAction(e -> settings.setResolution(resolutionComboBox.getValue()));

        HBox resolutionRow = new HBox(18, resolutionLabel, resolutionComboBox);
        resolutionRow.setAlignment(Pos.CENTER_LEFT);

        fullscreenCheckBox = new CheckBox("Fullscreen");
        fullscreenCheckBox.setSelected(settings.isFullscreen());
        fullscreenCheckBox.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");
        fullscreenCheckBox.setOnAction(e -> settings.setFullscreen(fullscreenCheckBox.isSelected()));

        soundToggle = new CheckBox("Sound On");
        soundToggle.setSelected(settings.isSoundEnabled());
        soundToggle.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");
        soundToggle.setOnAction(e -> settings.setSoundEnabled(soundToggle.isSelected()));

        Label volumeLabel = new Label("Volume:");
        volumeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");
        volumeSlider = new Slider(0, 100, settings.getVolume());
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setMajorTickUnit(20);
        volumeSlider.setMinorTickCount(4);
        volumeSlider.setPrefWidth(420);
        volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            settings.setVolume(newValue.intValue());
            volumeValueLabel.setText(settings.getVolume() + "%");
        });

        volumeValueLabel = new Label(settings.getVolume() + "%");
        volumeValueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

        HBox volumeRow = new HBox(18, volumeLabel, volumeSlider, volumeValueLabel);
        volumeRow.setAlignment(Pos.CENTER_LEFT);

        Button applyButton = new Button("Apply");
        Button backButton = new Button("Back to Menu");

        String buttonStyle = "-fx-background-color: #222; -fx-text-fill: #00ff00; -fx-font-size: 28px; " +
                             "-fx-pref-width: 260px; -fx-pref-height: 64px; -fx-border-color: #00ff00; -fx-border-width: 2px; -fx-cursor: hand;";
        applyButton.setStyle(buttonStyle);
        backButton.setStyle(buttonStyle);

        applyButton.setOnAction(e -> applySelectedSettings());
        backButton.setOnAction(e -> {
            applySelectedSettings();
            showMenuScreen();
        });

        HBox actionRow = new HBox(22, applyButton, backButton);
        actionRow.setAlignment(Pos.CENTER);

        VBox layout = new VBox(26, titleLabel, resolutionRow, fullscreenCheckBox, soundToggle, volumeRow, actionRow);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setPadding(new Insets(60));
        layout.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        return layout;
    }

    private void refreshOptionsControls() {
        if (resolutionComboBox != null) {
            resolutionComboBox.setValue(settings.getResolution());
        }
        if (fullscreenCheckBox != null) {
            fullscreenCheckBox.setSelected(settings.isFullscreen());
        }
        if (soundToggle != null) {
            soundToggle.setSelected(settings.isSoundEnabled());
        }
        if (volumeSlider != null) {
            volumeSlider.setValue(settings.getVolume());
        }
        if (volumeValueLabel != null) {
            volumeValueLabel.setText(settings.getVolume() + "%");
        }
    }

    private void showMenuScreen() {
        menuLayout.setVisible(true);
        optionsLayout.setVisible(false);
    }

    private void showOptionsScreen() {
        refreshOptionsControls();
        menuLayout.setVisible(false);
        optionsLayout.setVisible(true);
    }

    private void showGameScreen() {
        Label gameTitle = new Label("Game Running... (Press ESC to return to menu)");
        gameTitle.setStyle("-fx-font-size: 42px; -fx-text-fill: white;");

        StackPane gameScreen = new StackPane(gameTitle);
        gameScreen.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        gameScreen.setStyle("-fx-background-color: #000000;");

        gameScreen.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ESCAPE")) {
                showMenuScreen();
            }
        });

        gameScreen.requestFocus();

        menuLayout.setVisible(false);
        optionsLayout.setVisible(false);
        ((StackPane) scaleGroup.getChildren().get(0)).getChildren().clear();
        ((StackPane) scaleGroup.getChildren().get(0)).getChildren().add(gameScreen);
    }

    private void applySelectedSettings() {
        if (resolutionComboBox != null && resolutionComboBox.getValue() != null) {
            settings.setResolution(resolutionComboBox.getValue());
        }
        settings.setFullscreen(fullscreenCheckBox != null && fullscreenCheckBox.isSelected());
        settings.setSoundEnabled(soundToggle != null && soundToggle.isSelected());
        settings.setVolume((int) volumeSlider.getValue());

        window.setFullScreen(settings.isFullscreen());

        if (!settings.isFullscreen()) {
            window.setWidth(settings.getWidth());
            window.setHeight(settings.getHeight());
            window.centerOnScreen();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}


