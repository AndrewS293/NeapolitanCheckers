package com.checkers.controller;

import com.checkers.websocket.GameRoom;
import com.checkers.websocket.GameSessionManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameSessionManager sessionManager;

    public GameController(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @PostMapping
    public ResponseEntity<?> createGame() {

        String gameId = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        GameRoom room = sessionManager.createRoom(gameId);

        return ResponseEntity.ok(
                Map.of(
                        "gameId", room.getGameId(),
                        "status", room.getStatus().name(),
                        "players", room.getPlayerCount()
                )
        );
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<?> joinGame(@PathVariable String gameId) {

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Game not found"));
        }

        if (room.isFull()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Game is full"));
        }

        return ResponseEntity.ok(
                Map.of(
                        "gameId", room.getGameId(),
                        "status", room.getStatus().name(),
                        "players", room.getPlayerCount()
                )
        );
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGame(@PathVariable String gameId) {

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Game not found"));
        }

        return ResponseEntity.ok(
                Map.of(
                        "gameId", room.getGameId(),
                        "status", room.getStatus().name(),
                        "players", room.getPlayerCount()
                )
        );
    }
}