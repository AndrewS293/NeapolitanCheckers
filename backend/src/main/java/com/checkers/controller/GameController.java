package com.checkers.controller;

import com.checkers.database.MoveDAO;
import com.checkers.websocket.GameRoom;
import com.checkers.websocket.GameSessionManager;
import com.checkers.websocket.GameVisibility;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameSessionManager sessionManager;

    public GameController(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @PostMapping
    public ResponseEntity<?> createGame(@RequestAttribute GameVisibility visibility) {

        String gameId = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        String joinCode = null;

        if (visibility == GameVisibility.PRIVATE) {
            joinCode = UUID.randomUUID()
                    .toString()
                    .substring(0, 6)
                    .toUpperCase();
        }

        GameRoom room = sessionManager.createRoom(gameId, visibility, joinCode);

        if (joinCode != null) {
            return ResponseEntity.ok(
                    Map.of(
                            "gameId", room.getGameId(),
                            "joinCode", room.getJoinCode(),
                            "status", room.getStatus().name(),
                            "players", room.getPlayerCount()
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "gameId", room.getGameId(),
                        "status", room.getStatus().name(),
                        "players", room.getPlayerCount()
                )
        );
    }

    @GetMapping("/open")
    public ResponseEntity<?> getOpenGames() {

        List<Map<String, Object>> games = sessionManager
                .getOpenPublicGames()
                .stream()
                .map(room -> Map.<String, Object>of(
                        "gameId", room.getGameId(),
                        "status", room.getStatus().name(),
                        "players", room.getPlayerCount()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(games);
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<?> joinPublicGame(@PathVariable String gameId) {

        GameRoom room = sessionManager.getRoom(gameId);

        if (room == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Game not found"));
        }

        if (room.getVisibility() != GameVisibility.PUBLIC) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Game is not public"));
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

    @PostMapping ("/join/{joinCode}")
    public ResponseEntity<?> joinPrivateGame(@PathVariable String joinCode) {

        GameRoom room = sessionManager.getRoomByJoinCode(joinCode);

        if (room == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Invalid join code"));
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
    @GetMapping("/test-move")
public String testMove() {

    MoveDAO.saveMove(
        1,
        13,
        4,
        "f6",
        "g5"
    );

    return "Move sent to Supabase!";
 }
}