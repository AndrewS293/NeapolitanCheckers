package com.checkers.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

    // Reads all saved moves in ascending move-number order.
    public static ArrayNode getMovesForReplay(long gameId) {

        if (gameId <= 0) {
            throw new IllegalArgumentException("Invalid game ID.");
        }

        String secretKey = SupabaseConfig.API_KEY;

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "Replay database is not configured."
            );
        }

        ArrayNode allMoves = objectMapper.createArrayNode();
        int lastMoveNumber = 0;

        try {
            while (true) {

                String url = SupabaseConfig.URL
                        + "/rest/v1/game_moves"
                        + "?game_id=eq." + gameId
                        + "&select=game_id,player_id,move_number,move_data"
                        + "&order=move_number.asc"
                        + "&move_number=gt." + lastMoveNumber
                        + "&limit=500";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("apikey", secretKey)
                        .header("Authorization", "Bearer " + secretKey)
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {
                    throw new IllegalStateException(
                            "Could not retrieve saved moves."
                    );
                }

                JsonNode page = objectMapper.readTree(response.body());

                if (page == null || !page.isArray()) {
                    throw new IllegalStateException(
                            "Invalid replay response."
                    );
                }

                if (page.isEmpty()) {
                    return allMoves;
                }

                for (JsonNode savedMove : page) {
                    JsonNode number = savedMove.get("move_number");

                    if (number == null
                            || !number.isIntegralNumber()
                            || !number.canConvertToInt()
                            || number.asInt() != lastMoveNumber + 1) {
                        throw new IllegalStateException(
                                "Saved move sequence is incomplete."
                        );
                    }

                    allMoves.add(savedMove);
                    lastMoveNumber = number.asInt();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Replay retrieval was interrupted."
            );

        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Could not read saved moves."
            );
        }
    }
}