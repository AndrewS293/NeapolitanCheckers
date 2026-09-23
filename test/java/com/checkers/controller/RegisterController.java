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

        message.setWrapText(true);

        Button registerButton =
                new Button("Register");

        Button backButton =
                new Button("Back to Login");


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
                        "Account created successfully!"
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
                            "Registration failed.";
                }

                message.setText(error);
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

        VBox root =
                new VBox(15);

        root.setPadding(
                new Insets(40)
        );

        root.setAlignment(
                Pos.CENTER
        );

        root.setMaxWidth(400);

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