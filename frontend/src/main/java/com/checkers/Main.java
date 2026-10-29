package com.checkers;

import com.checkers.controller.GameBoardController;
import com.checkers.api.AuthApi;
import com.checkers.controller.LoginController;
import com.checkers.controller.MainMenuController;
import com.checkers.controller.RegisterController;
import com.checkers.api.GameApi;
import com.checkers.controller.GameLobbyController;
import com.checkers.controller.VariantSelectionController;
import com.checkers.logic.GameLogic;
import com.checkers.variants.GameFactory;
import com.checkers.variants.VariantRegistry;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage stage;

    private static final AuthApi authApi =
            new AuthApi();

    private static final GameApi gameApi =
            new GameApi(authApi);

    @Override
    public void start(Stage primaryStage) {

        stage = primaryStage;

        stage.setTitle("Neapolitan Checkers");

        showLogin();

        stage.show();
    }

    public static void showGameBoard() {

        GameBoardController controller =
                new GameBoardController();

        Scene scene = controller.createScene();

        stage.setScene(scene);

        stage.setWidth(700);
        stage.setHeight(900);
    }

    public static AuthApi getAuthApi() {
        return authApi;
    }

    public static void showLogin() {

        LoginController controller =
                new LoginController(authApi);

        Scene scene = controller.createScene();

        stage.setScene(scene);

        stage.setWidth(540);
        stage.setHeight(700);
    }

    public static void showRegister() {

        RegisterController controller =
                new RegisterController(authApi);

        Scene scene = controller.createScene();

        stage.setScene(scene);

        stage.setWidth(540);
        stage.setHeight(700);
    }

    public static void showMainMenu() {

        MainMenuController controller =
                new MainMenuController(authApi);

        Scene scene = controller.createScene();

        stage.setScene(scene);

        stage.setWidth(540);
        stage.setHeight(700);
    }

    public static void showGameLobby() {

        GameLobbyController controller =
                new GameLobbyController();

        Scene scene = controller.createScene();

        stage.setScene(scene);
        stage.setWidth(540);
        stage.setHeight(700);
    }

    
    
    public static void showOnlineGameBoard(GameBoardController controller) {
        Scene scene = controller.createScene();
        stage.setScene(scene);
        stage.setWidth(700);
        stage.setHeight(900);
    }

    
    public static void showVariantSelection() {
        VariantSelectionController controller =
                new VariantSelectionController(stage);
        controller.show();
    }

    public static void showGameBoard(String variantId) {
        GameFactory factory =
                new GameFactory(VariantRegistry.defaults());

        GameLogic game = factory.createGame(variantId);

        GameBoardController controller =
                new GameBoardController(game);

        stage.setScene(controller.createScene());
        stage.setTitle("Neapolitan Checkers - " + variantId);
        stage.show();
    }



    public static void main(String[] args) {
        launch(args);
    }
}