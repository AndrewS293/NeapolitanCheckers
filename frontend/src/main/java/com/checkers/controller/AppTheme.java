package com.checkers.controller;

import javafx.scene.Parent;
import javafx.scene.control.Button;

public final class AppTheme {
    public static final String BACKGROUND = "#e9eee9";
    public static final String SURFACE = "#ffffff";
    public static final String INK = "#173c36";
    public static final String MUTED = "#52635d";
    public static final String PRIMARY = "#245c50";
    public static final String ACCENT = "#a64b3c";
    public static final String BORDER = "#cbd6ce";

    private AppTheme() { }

    public static void applyBackground(Parent root) {
        root.setStyle("-fx-background-color: " + BACKGROUND + ";"
                + " -fx-font-family: 'Georgia';");
    }

    public static void stylePrimaryButton(Button button) {
        button.setStyle("-fx-background-color: " + PRIMARY + ";"
                + " -fx-text-fill: white; -fx-font-size: 14px;"
                + " -fx-background-radius: 5px;");
    }

    public static void styleSecondaryButton(Button button) {
        button.setStyle("-fx-background-color: " + SURFACE + ";"
                + " -fx-text-fill: " + INK + "; -fx-font-size: 14px;"
                + " -fx-border-color: " + BORDER + ";"
                + " -fx-background-radius: 5px; -fx-border-radius: 5px;");
    }
}