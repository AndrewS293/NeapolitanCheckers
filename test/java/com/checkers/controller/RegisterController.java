package com.checkers.controller;

import com.checkers.Main;
import com.checkers.api.AuthApi;
import com.checkers.model.User;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterController {

    private final AuthApi authApi;

    public RegisterController(AuthApi authApi) {
        this.authApi = authApi;
    }

    public Scene createScene() {

        Label title =
                new Label("Create Account");

        title.setStyle(
                "-fx-font-size: 26px; " +
                "-fx-font-weight: bold;"
        );

        TextField usernameField =
                new TextField();

        usernameField.setPromptText("Username");

        TextField emailField =
                new TextField();

        emailField.setPromptText("Email");

        PasswordField passwordField =
                new PasswordField();

        passwordField.setPromptText("Password");

        PasswordField confirmField =
                new PasswordField();

        confirmField.setPromptText(
                "Confirm Password"
        );

        Label message =
                new Label();

        Button registerButton =
                new Button("Register");

        Button backButton =
                new Button("Back to Login");

        registerButton.setOnAction(event -> {

            String username =
                    usernameField.getText().trim();

            String email =
                    emailField.getText().trim();

            String password =
                    passwordField.getText();

            String confirm =
                    confirmField.getText();

            if (username.isEmpty()
                    || email.isEmpty()
                    || password.isEmpty()) {

                message.setText(
                        "Please fill in all fields."
                );

                return;
            }

            if (!password.equals(confirm)) {

                message.setText(
                        "Passwords do not match."
                );

                return;
            }

            try {

                User user =
                        authApi.register(
                                username,
                                email,
                                password
                        );

                // Registration succeeded.
                // Do NOT log the user in automatically.
                message.setText(
                        "Account created successfully!"
                );

                Main.showLogin();

            } catch (Exception e) {

                message.setText(
                        "Registration failed."
                );
            }
        });

        backButton.setOnAction(event ->
                Main.showLogin()
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
                usernameField,
                emailField,
                passwordField,
                confirmField,
                registerButton,
                backButton,
                message
        );

        return new Scene(root);
    }
}