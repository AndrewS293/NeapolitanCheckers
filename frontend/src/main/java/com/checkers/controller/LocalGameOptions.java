package com.checkers.controller;

import java.util.Objects;

public record LocalGameOptions(
        Opponent opponent,
        ReinforcementLearningTrainer.Difficulty difficulty) {

    public enum Opponent {
        HUMAN,
        AI
    }

    public LocalGameOptions {
        Objects.requireNonNull(opponent, "Opponent is required.");
        if (opponent == Opponent.AI) {
            Objects.requireNonNull(difficulty, "AI difficulty is required.");
        } else if (difficulty != null) {
            throw new IllegalArgumentException(
                    "Human multiplayer does not use an AI difficulty.");
        }
    }

    public static LocalGameOptions human() {
        return new LocalGameOptions(Opponent.HUMAN, null);
    }

    public static LocalGameOptions ai(
            ReinforcementLearningTrainer.Difficulty difficulty) {
        return new LocalGameOptions(Opponent.AI, difficulty);
    }
}