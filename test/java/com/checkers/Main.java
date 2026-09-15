package com.checkers;

import com.checkers.api.AuthApi;
import com.checkers.controller.LoginController;
import com.checkers.controller.MainMenuController;
import com.checkers.controller.RegisterController;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage stage;

    private static final AuthApi authApi =
            new AuthApi();

    @Override
    public void start(Stage primaryStage) {

        stage = primaryStage;

        stage.setTitle("Neapolitan Checkers");

        showLogin();

        stage.show();
    }

    public static AuthApi getAuthApi() {
        return authApi;
    }

    public static void showLogin() {

        LoginController controller =
                new LoginController(authApi);

        Scene scene = controller.createScene();

        stage.setScene(scene);

        stage.setWidth(500);
        stage.setHeight(450);
    }

    public static void showRegister() {

        RegisterController controller =
                new RegisterController(authApi);

        Scene scene = controller.createScene();

        stage.setScene(scene);

        stage.setWidth(500);
        stage.setHeight(500);
    }

    public static void showMainMenu() {

        MainMenuController controller =
                new MainMenuController(authApi);

        Scene scene = controller.createScene();

        stage.setScene(scene);

        stage.setWidth(500);
        stage.setHeight(450);
    }

    public static void main(String[] args) {
        launch(args);
    }
}