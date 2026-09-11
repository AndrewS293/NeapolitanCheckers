package com.checkers;

public class GameSettings {
    private int width = 800;
    private int height = 600;
    private boolean fullscreen = false;
    private boolean soundEnabled = true;
    private int volume = 80;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public int getVolume() {
        return volume;
    }

    public String getResolution() {
        return width + "x" + height;
    }

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

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

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
