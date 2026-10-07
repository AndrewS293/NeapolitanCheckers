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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RegisterController {

    private final AuthApi authApi;

    public RegisterController(AuthApi authApi) {
        this.authApi = authApi;
    }

    public Scene createScene() {

        Label title =
                new Label("Create your account");

        title.setStyle(
                "-fx-font-size: 28px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #17324D;"
        );

        Label subtitle =
                new Label("");

        subtitle.setStyle(
                "-fx-font-size: 13px; " +
                "-fx-text-fill: #5F7485;"
        );

        Label badge =
                new Label("♟  WELCOME TO NEAPOLITAN CHECKERS  ♟ ");

        badge.setStyle(
                "-fx-background-color: #FBE9D7; " +
                "-fx-text-fill: #A9492B; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6 11 6 11; " +
                "-fx-background-radius: 20;"
        );

        TextField usernameField =
                new TextField();

        usernameField.setPromptText("Username");

        TextField emailField =
                new TextField();

        emailField.setPromptText("Email address");

        PasswordField passwordField =
                new PasswordField();

        passwordField.setPromptText("Password");

        PasswordField confirmField =
                new PasswordField();

        confirmField.setPromptText(
                "Confirm password"
        );

        Label passwordHint =
                new Label("Password must be 8+ characters with uppercase, lowercase, and a number.");

        passwordHint.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #6B7A87;"
        );

        Label message =
                new Label();

        message.setWrapText(true);
        message.setStyle(
                "-fx-font-size: 12px; " +
                "-fx-text-fill: #A9492B;"
        );

        Button registerButton =
                new Button("Create account");

        registerButton.setStyle(
                "-fx-background-color: #C95D3A; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10 25 10 25; " +
                "-fx-background-radius: 10;"
        );

        Button backButton =
                new Button("Back to login");

        backButton.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #31546A; " +
                "-fx-font-size: 12px;"
        );


        /*
         * REALTIME VALIDATION
         */

        usernameField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    validateUsername(
                            usernameField,
                            message
                    );
                }
        );

        emailField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    validateEmail(
                            emailField,
                            message
                    );
                }
        );

        passwordField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    validatePassword(
                            passwordField,
                            message
                    );

                    validateConfirmPassword(
                            passwordField,
                            confirmField,
                            message
                    );
                }
        );

        confirmField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    validateConfirmPassword(
                            passwordField,
                            confirmField,
                            message
                    );
                }
        );


        /*
         * REGISTER
         */

        registerButton.setOnAction(event -> {

            String username =
                    usernameField.getText().trim();

            String email =
                    emailField.getText().trim();

            String password =
                    passwordField.getText();

            String confirm =
                    confirmField.getText();


            /*
             * Validate everything one more time
             * before contacting the server.
             */

            if (!validateUsername(
                    usernameField,
                    message)) {
                return;
            }

            if (!validateEmail(
                    emailField,
                    message)) {
                return;
            }

            if (!validatePassword(
                    passwordField,
                    message)) {
                return;
            }

            if (!validateConfirmPassword(
                    passwordField,
                    confirmField,
                    message)) {
                return;
            }


            /*
             * Send registration request
             */

            try {

                User user =
                        authApi.register(
                                username,
                                email,
                                password
                        );

                message.setText(
                        "Account created successfully! " +
                        "You can now sign in."
                );
                message.setStyle(
                        "-fx-font-size: 12px; " +
                        "-fx-text-fill: #247A55;"
                );

                // Do NOT automatically log in.
                Main.showLogin();

            } catch (Exception e) {

                /*
                 * Display the actual error returned
                 * by the backend.
                 */

                String error =
                        e.getMessage();

                if (error == null ||
                        error.isBlank()) {

                    error =
                            "Registration failed. Please try again.";
                }

                message.setText(error);
                message.setStyle(
                        "-fx-font-size: 12px; " +
                        "-fx-text-fill: #A9492B;"
                );
            }
        });


        /*
         * BACK TO LOGIN
         */

        backButton.setOnAction(event ->
                Main.showLogin()
        );


        /*
         * LAYOUT
         */

        VBox form =
                new VBox(14);

        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(430);
        form.getChildren().addAll(
                badge,
                title,
                subtitle,
                usernameField,
                emailField,
                passwordField,
                confirmField,
                passwordHint,
                registerButton,
                backButton,
                message
        );

        VBox card =
                new VBox(0);

        card.setStyle(
                "-fx-background-color: #FFFDF8; " +
                "-fx-background-radius: 24; " +
                "-fx-border-color: #E9D8C8; " +
                "-fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(23, 50, 77, 0.16), 12, 0.35, 0, 4);"
        );
        card.setPadding(new Insets(34, 38, 30, 38));
        card.getChildren().add(form);

        StackPane root =
                new StackPane(card);

        root.setStyle(
                "-fx-background-color: #F6EDE2; " +
                "-fx-background-radius: 28;"
        );
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));

        return new Scene(root, 560, 650);
    }


    /*
     * USERNAME VALIDATION
     */

    private boolean validateUsername(
            TextField usernameField,
            Label message) {

        String username =
                usernameField.getText().trim();

        if (username.isEmpty()) {

            message.setText(
                    "Username is required."
            );

            return false;
        }

        if (username.length() < 3) {

            message.setText(
                    "Username must be at least 3 characters."
            );

            return false;
        }

        if (username.length() > 50) {

            message.setText(
                    "Username cannot exceed 50 characters."
            );

            return false;
        }

        if (!username.matches(
                "^[a-zA-Z0-9_]+$")) {

            message.setText(
                    "Username can only contain letters, numbers, and underscores."
            );

            return false;
        }

        message.setText("");

        return true;
    }


    /*
     * EMAIL VALIDATION
     */

    private boolean validateEmail(
            TextField emailField,
            Label message) {

        String email =
                emailField.getText().trim();

        if (email.isEmpty()) {

            message.setText(
                    "Email is required."
            );

            return false;
        }

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            message.setText(
                    "Please enter a valid email address."
            );

            return false;
        }

        message.setText("");

        return true;
    }


    /*
     * PASSWORD VALIDATION
     */

    private boolean validatePassword(
            PasswordField passwordField,
            Label message) {

        String password =
                passwordField.getText();

        if (password.isEmpty()) {

            message.setText(
                    "Password is required."
            );

            return false;
        }

        if (password.length() < 8) {

            message.setText(
                    "Password must be at least 8 characters."
            );

            return false;
        }

        if (password.length() > 50) {

            message.setText(
                    "Password cannot exceed 50 characters."
            );

            return false;
        }

        if (password.contains(" ")) {

            message.setText(
                    "Password cannot contain spaces."
            );

            return false;
        }


        if (password.matches(".*[<>\"'%;)(&+].*")) {

            message.setText(
                    "Password cannot contain special characters like <, >, \", ', %, ;, ), (, &, +"
            );

            return false;
        }


        if (password.matches(".*[A-Z].*") &&
            password.matches(".*[a-z].*") &&
            password.matches(".*\\d.*")) {
        } else {
            message.setText(
                    "Password must contain at least one uppercase letter, one lowercase letter, and one number."
            );

            return false;
        }

        message.setText("");

        return true;
    }


    /*
     * CONFIRM PASSWORD VALIDATION
     */

    private boolean validateConfirmPassword(
            PasswordField passwordField,
            PasswordField confirmField,
            Label message) {

        String password =
                passwordField.getText();

        String confirm =
                confirmField.getText();

        if (confirm.isEmpty()) {

            message.setText(
                    "Please confirm your password."
            );

            return false;
        }

        if (!password.equals(confirm)) {

            message.setText(
                    "Passwords do not match."
            );

            return false;
        }

        message.setText("");

        return true;
    }
}