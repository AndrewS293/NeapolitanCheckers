package com.checkers.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class GameReplay {

    // Frame 0 is the starting board.
    // Frame 1 is the board after move 1, and so on.
    private final List<GameLogic> frames = new ArrayList<>();

    private int position = 0;
    private boolean playing = false;

    public GameReplay(ArrayNode savedMoves) {

        if (savedMoves == null) {
            throw new IllegalArgumentException(
                    "Saved moves are required."
            );
        }

        GameLogic replayGame = new GameLogic();
        frames.add(replayGame.copy());

        int expectedMoveNumber = 1;

        for (JsonNode savedMove : savedMoves) {

            JsonNode moveNumber = savedMove.path("move_number");
            JsonNode moveData = savedMove.path("move_data");

            if (!moveNumber.isIntegralNumber()
                    || !moveNumber.canConvertToInt()
                    || moveNumber.asInt() != expectedMoveNumber) {

                throw new IllegalArgumentException(
                        "Saved moves must be numbered in order."
                );
            }

            JsonNode startNode = moveData.path("start");
            JsonNode endNode = moveData.path("end");

            if (!startNode.isIntegralNumber()
                    || !startNode.canConvertToInt()
                    || !endNode.isIntegralNumber()
                    || !endNode.canConvertToInt()) {

                throw new IllegalArgumentException(
                        "A saved move is missing valid start/end indices."
                );
            }

            int start = startNode.asInt();
            int end = endNode.asInt();

            if (!replayGame.move(start, end)) {
                throw new IllegalArgumentException(
                        "Saved move " + expectedMoveNumber
                                + " cannot be replayed."
                );
            }

            frames.add(replayGame.copy());
            expectedMoveNumber++;
        }
    }

    // Returns a copy so callers cannot change the stored frame.
    public synchronized GameLogic getCurrentGame() {
        return frames.get(position).copy();
    }

    public synchronized int getPosition() {
        return position;
    }

    public int getMoveCount() {
        return frames.size() - 1;
    }

    public synchronized boolean hasNext() {
        return position < getMoveCount();
    }

    public synchronized boolean hasPrevious() {
        return position > 0;
    }

    public synchronized GameLogic next() {
        if (hasNext()) {
            position++;
        }

        if (!hasNext()) {
            playing = false;
        }

        return getCurrentGame();
    }

    public synchronized GameLogic previous() {
        playing = false;

        if (hasPrevious()) {
            position--;
        }

        return getCurrentGame();
    }

    public synchronized GameLogic restart() {
        playing = false;
        position = 0;
        return getCurrentGame();
    }

    public synchronized GameLogic seek(int moveNumber) {
        if (moveNumber < 0 || moveNumber > getMoveCount()) {
            throw new IllegalArgumentException(
                    "Replay position is outside the saved game."
            );
        }

        playing = false;
        position = moveNumber;
        return getCurrentGame();
    }

    public synchronized void play() {
        playing = hasNext();
    }

    public synchronized void pause() {
        playing = false;
    }

    public synchronized boolean isPlaying() {
        return playing;
    }

    // A timer can call this method to advance playback.
    public synchronized GameLogic tick() {
        if (playing) {
            return next();
        }

        return getCurrentGame();
    }
}