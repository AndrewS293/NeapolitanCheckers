package com.checkers.database;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class MoveDAO {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Keeps existing calls using "from" and "to" working.
    public static boolean saveMove(
            long gameId,
            long playerId,
            int moveNumber,
            String from,
            String to) {

        if (from == null || to == null
                || from.isBlank() || to.isBlank()) {
            return false;
        }

        Map<String, Object> moveData = Map.of(
                "notation", from + "-" + to,
                "from", from,
                "to", to,
                "capture", false,
                "king", false
        );

        return saveMove(gameId, playerId, moveNumber, moveData);
    }

    // Saves complete move information supplied by the server.
    public static boolean saveMove(
            long gameId,
            long playerId,
            int moveNumber,
            Map<String, Object> moveData) {

        if (gameId <= 0
                || playerId <= 0
                || moveNumber <= 0
                || moveData == null
                || moveData.isEmpty()) {
            return false;
        }

        String secretKey = SupabaseConfig.API_KEY;

        if (secretKey == null || secretKey.isBlank()) {
            return false;
        }

        try {
            Map<String, Object> moveEntry = Map.of(
                    "game_id", gameId,
                    "move_number", moveNumber,
                    "player_id", playerId,
                    "move_data", moveData
            );

            String json = objectMapper.writeValueAsString(moveEntry);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.URL + "/rest/v1/game_moves"
                    ))
                    .timeout(Duration.ofSeconds(15))
                    .header("apikey", secretKey)
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;

        } catch (Exception e) {
            return false;
        }
    }
}