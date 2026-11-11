package com.checkers.controller;

import com.checkers.Main;
import com.checkers.api.AuthApi;
import com.checkers.model.User;
import com.checkers.session.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MainMenuController {

    private final AuthApi authApi;

    public MainMenuController(AuthApi authApi) {
        this.authApi = authApi;
    }

    public Scene createScene() {

        User user =
                SessionManager.getCurrentUser();

        Label title =
                new Label("Neapolitan Checkers");

        title.setStyle(
                "-fx-font-size: 28px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + AppTheme.INK + ";"
        );

        Label welcome =
                new Label(
                        "Welcome, "
                        + user.getUsername()
                        + "!"
                );

        Button playButton =
                new Button("Play Locally"); 
        AppTheme.stylePrimaryButton(playButton);
        playButton.setOnAction(event -> {
                Main.showVariantSelection();
        });

        Button playOnlineButton =
                new Button("Play Online");
        AppTheme.stylePrimaryButton(playOnlineButton);
        playOnlineButton.setOnAction(event -> {
                Main.showGameLobby();
        });
                
        Button logoutButton =
                new Button("Logout");
        AppTheme.styleSecondaryButton(logoutButton);

        logoutButton.setOnAction(event -> {

            try {
                authApi.logout();
            } catch (Exception ignored) {
            }

            SessionManager.logout();

            Main.showLogin();
        });

        VBox root =
                new VBox(20);

        root.setPadding(
                new Insets(40)
        );

        root.setAlignment(
                Pos.CENTER
        );
        AppTheme.applyBackground(root);

        root.getChildren().addAll(
                title,
                welcome,
                playButton,
                playOnlineButton,
                logoutButton
        );

        return new Scene(root);
    }
}