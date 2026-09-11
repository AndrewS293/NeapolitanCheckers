package com.checkers;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GameApp extends Application {

    private Stage window;
    private Scene menuScene;
    private Scene gameScene;

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("My JavaFX Game");

        createMenuScene();

        createGameScene();

        window.setScene(menuScene);
        window.show();
    }

    private void createMenuScene() {
        
        Label titleLabel = new Label("Neapolitan Checkers");
        titleLabel.setStyle("-fx-font-size: 40px; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        
        Button startButton = new Button("PLAY");
        Button optionsButton = new Button("OPTIONS");
        Button exitButton = new Button("EXIT");

        String buttonStyle = "-fx-background-color: #222; -fx-text-fill: #00ff00; -fx-font-size: 18px; " +
                             "-fx-pref-width: 200px; -fx-border-color: #00ff00; -fx-border-width: 2px; -fx-cursor: hand;";
        startButton.setStyle(buttonStyle);
        optionsButton.setStyle(buttonStyle);
        exitButton.setStyle(buttonStyle);


        startButton.setOnMouseEntered(e -> startButton.setStyle(buttonStyle + "-fx-background-color: #00ff00; -fx-text-fill: #222;"));
        startButton.setOnMouseExited(e -> startButton.setStyle(buttonStyle));

        optionsButton.setOnMouseEntered(e -> optionsButton.setStyle(buttonStyle + "-fx-background-color: #00ff00; -fx-text-fill: #222;"));
        optionsButton.setOnMouseExited(e -> optionsButton.setStyle(buttonStyle));

        exitButton.setOnMouseEntered(e -> exitButton.setStyle(buttonStyle + "-fx-background-color: #00ff00; -fx-text-fill: #222;"));
        exitButton.setOnMouseExited(e -> exitButton.setStyle(buttonStyle));


        startButton.setOnAction(e -> window.setScene(gameScene)); 
        exitButton.setOnAction(e -> window.close());              

        VBox menuLayout = new VBox(20); 
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.getChildren().addAll(titleLabel, startButton, optionsButton, exitButton);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0d0d1a;"); 
        root.getChildren().add(menuLayout);

        menuScene = new Scene(root, 800, 600);
    }

    private void createGameScene() {
    
        StackPane gameRoot = new StackPane();
        gameRoot.setStyle("-fx-background-color: #000000;");

        Label placeholderLabel = new Label("Game Running... (Press ESC to return to main menu)");
        placeholderLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
        gameRoot.getChildren().add(placeholderLabel);

        gameScene = new Scene(gameRoot, 800, 600);

        
        gameScene.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ESCAPE")) {
                window.setScene(menuScene);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
