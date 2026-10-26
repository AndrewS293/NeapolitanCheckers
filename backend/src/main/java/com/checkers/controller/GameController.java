package com.checkers.controller;

import com.checkers.database.MoveDAO;
import com.checkers.database.GameDAO;
import com.checkers.websocket.GameRoom;
import com.checkers.websocket.GameSessionManager;
import com.checkers.websocket.GameVisibility;
import com.checkers.logic.GameReplay;
import com.checkers.logic.GameLogic;
import com.checkers.logic.Board;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@RestController

//Our endpoint for game creation
@RequestMapping("/api/games")
// implements gamecontroller component of application
public class GameController {

    private final GameSessionManager sessionManager;

    public GameController(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @PostMapping
    // creates a new game with the specified visibility
    public ResponseEntity<?> createGame(@RequestParam GameVisibility visibility) {

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

    // Create game in Supabase
    long databaseGameId = GameDAO.createGame(gameId);

    if (databaseGameId == -1) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error",
                        "Failed to create game in database"
                ));
    }

    // Create the WebSocket game room
    GameRoom room = sessionManager.createRoom(
            gameId,
            databaseGameId,
            visibility,
            joinCode
    );

    if (joinCode != null) {
        return ResponseEntity.ok(
                Map.of(
                        "gameId", room.getGameId(),
                        "databaseGameId", databaseGameId,
                        "joinCode", room.getJoinCode(),
                        "status", room.getStatus().name(),
                        "players", room.getPlayerCount()
                )
        );
    }

    return ResponseEntity.ok(
            Map.of(
                    "gameId", room.getGameId(),
                    "databaseGameId", databaseGameId,
                    "status", room.getStatus().name(),
                    "players", room.getPlayerCount()
            )
    );
}

        // Endpoint to get a list of open public games
    @GetMapping("/open")
    // retrieves a list of open public games
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

    // Endpoint to join a public game by gameId
    @PostMapping("/{gameId}/join")
    // joins a public game by its game ID
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

    // Endpoint to join a private game by joinCode
    @PostMapping ("/join/{joinCode}")
    // joins a private game by its join code
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


    // Endpoint to get game details by gameId
    @GetMapping("/{gameId}")
    // retrieves a game by its game ID
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
        // Returns saved moves, or a board at a selected replay position.
    @GetMapping("/{databaseGameId}/replay")
    public ResponseEntity<?> getReplay(
            @PathVariable("databaseGameId") long databaseGameId,
            @RequestParam(value = "position", required = false)
            Integer position,
            HttpSession session) {

        Object identity = session.getAttribute("userId");

        if (!(identity instanceof Number)
                || ((Number) identity).longValue() <= 0) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please log in first."));
        }

        if (databaseGameId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid database game ID."));
        }

        long userId = ((Number) identity).longValue();

        try {
            var savedMoves = MoveDAO.getMovesForReplay(databaseGameId);

            // Only a player with a saved move may view this replay.
            boolean participant = false;

            for (var savedMove : savedMoves) {
                if (savedMove.path("player_id").asLong(-1) == userId) {
                    participant = true;
                    break;
                }
            }

            if (!participant) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error",
                                "Replay is available to players "
                                        + "who made a move in this game."
                        ));
            }

            // Without a position, return the ordered move history.
            if (position == null) {
                return ResponseEntity.ok(
                        Map.of(
                                "databaseGameId", databaseGameId,
                                "moveCount", savedMoves.size(),
                                "moves", savedMoves
                        )
                );
            }

            if (position < 0 || position > savedMoves.size()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Position must be between 0 and "
                                        + savedMoves.size() + "."
                        ));
            }

            GameReplay replay = new GameReplay(savedMoves);
            GameLogic frame = replay.seek(position);

            int[] board = new int[Board.playable_squares];

            for (int index = 0; index < board.length; index++) {
                board[index] = frame.getBoard()
                        .getPiece(index).getType();
            }

            return ResponseEntity.ok(
                    Map.of(
                            "databaseGameId", databaseGameId,
                            "position", position,
                            "moveCount", replay.getMoveCount(),
                            "board", board,
                            "currentPlayer", frame.getCurrentPlayer(),
                            "forcedJumpIndex", frame.getForcedJumpIndex(),
                            "gameState", frame.getGameState().name()
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            "This saved game cannot be reconstructed "
                                    + "from its move data."
                    ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error",
                            "Replay is temporarily unavailable."
                    ));
        }
    }
}