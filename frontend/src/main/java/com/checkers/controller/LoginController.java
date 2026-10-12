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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {

    private final AuthApi authApi;

    public LoginController(AuthApi authApi) {
        this.authApi = authApi;
    }

    public Scene createScene() {

        Label title =
                new Label("Neapolitan Checkers");

        title.setStyle(
                "-fx-font-size: 28px; " +
                "-fx-font-weight: bold;"
        );

        Label subtitle =
                new Label("Login");

        TextField usernameField =
                new TextField();

        usernameField.setPromptText("Username");

        PasswordField passwordField =
                new PasswordField();

        passwordField.setPromptText("Password");

        Label message =
                new Label();

        Button loginButton =
                new Button("Login");

        Button registerButton =
                new Button("Create Account");

        loginButton.setOnAction(event -> {

            try {

                User user =
                        authApi.login(
                                usernameField.getText(),
                                passwordField.getText()
                        );

                SessionManager.login(user);

                Main.showMainMenu();

            } catch (Exception e) {

                message.setText(
                        "Invalid username or password."
                );
            }
        });

        registerButton.setOnAction(event ->
                Main.showRegister()
        );

        VBox root =
                new VBox(15);

        root.setPadding(
                new Insets(40)
        );

        root.setAlignment(
                Pos.CENTER
        );

        root.getChildren().addAll(
                title,
                subtitle,
                usernameField,
                passwordField,
                loginButton,
                registerButton,
                message
        );

        return new Scene(root);
    }
}