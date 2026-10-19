package com.checkers;

// Represents the settings for the game, including window size, fullscreen mode, sound, and volume.
// Provides getters and setters for each setting, with validation for volume and resolution.
public class GameSettings {
    private int width = 800;
    private int height = 600;
    private boolean fullscreen = false;
    private boolean soundEnabled = true;
    private int volume = 80;

    // Default constructor initializes the settings with default values.
    public int getWidth() {
        return width;
    }
 
    // Returns the height of the game window.
    public int getHeight() {
        return height;
    }

    // Returns whether the game is in fullscreen mode.
    public boolean isFullscreen() {
        return fullscreen;
    }

    // Returns whether sound is enabled in the game.
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    // Returns the volume level of the game, ranging from 0 to 100.
    public int getVolume() {
        return volume;
    }

    // Returns the resolution of the game window in the format "widthxheight".
    public String getResolution() {
        return width + "x" + height;
    }

    // Sets the resolution of the game window in the format "widthxheight".
    // Validates the input and only updates the width and height if the input is valid.
    public void setResolution(String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            return;
        }

        String normalized = resolution.trim().toLowerCase();
        String[] parts = normalized.split("x");
        if (parts.length != 2) {
            return;
        }

        try {
            int newWidth = Integer.parseInt(parts[0].trim());
            int newHeight = Integer.parseInt(parts[1].trim());
            if (newWidth > 0 && newHeight > 0) {
                this.width = newWidth;
                this.height = newHeight;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    // Sets whether the game is in fullscreen mode.
    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    // Sets whether sound is enabled in the game.
    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    // Sets the volume level of the game, ranging from 0 to 100.
    // Validates the input and clamps it to the valid range.
    public void setVolume(int volume) {
        if (volume < 0) {
            this.volume = 0;
            return;
        }
        if (volume > 100) {
            this.volume = 100;
            return;
        }
        this.volume = volume;
    }
}
