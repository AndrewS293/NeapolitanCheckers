package com.checkers.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Supabase persistence operations for game metadata and participants. */
public final class GameDAO {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private GameDAO() {}

    private static HttpRequest.Builder request(String path) {
        String key = SupabaseConfig.API_KEY;
        if (key == null || key.isBlank() || SupabaseConfig.URL == null || SupabaseConfig.URL.isBlank())
            throw new IllegalStateException("Supabase URL/API key is not configured.");
        return HttpRequest.newBuilder(URI.create(SupabaseConfig.URL + "/rest/v1/" + path))
                .timeout(Duration.ofSeconds(20)).header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Accept", "application/json");
    }

    public static long createGame(String roomCode, String variantId) {
        if (variantId == null || variantId.isBlank()) throw new IllegalArgumentException("Variant is required.");
        try {
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("room_code", roomCode); body.put("variant_id", variantId.trim().toUpperCase(Locale.ROOT));
            body.put("status", "IN_PROGRESS");
            HttpRequest req = request("games?select=id")
                    .header("Content-Type", "application/json").header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 201) { logFailure("createGame", res); return -1; }
            JsonNode arr = JSON.readTree(res.body());
            return arr.isArray() && !arr.isEmpty() ? arr.get(0).path("id").asLong(-1) : -1;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); System.err.println("createGame interrupted: " + e.getMessage()); return -1; }
          catch (Exception e) { System.err.println("createGame failed: " + e.getMessage()); return -1; }
    }

    /** Idempotently registers a player in game_players. */
    public static boolean registerPlayer(long gameId, long userId, int playerNumber) {
        if (gameId <= 0 || userId <= 0 || playerNumber < 1 || playerNumber > 2) return false;
        try {
            Map<String,Object> body = Map.of("game_id", gameId, "user_id", userId, "player_number", playerNumber);
            HttpRequest req = request("game_players?on_conflict=game_id,user_id")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) logFailure("registerPlayer", res);
            return res.statusCode() >= 200 && res.statusCode() < 300;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
          catch (Exception e) { System.err.println("registerPlayer failed for game="+gameId+", user="+userId+": "+e.getMessage()); return false; }
    }

    public static boolean isParticipant(long gameId, long userId) {
        try {
            String path = "game_players?select=user_id&game_id=eq."+gameId+"&user_id=eq."+userId+"&limit=1";
            HttpResponse<String> res = CLIENT.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) { logFailure("isParticipant",res); return false; }
            JsonNode rows = JSON.readTree(res.body()); return rows.isArray() && !rows.isEmpty();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
          catch (Exception e) { System.err.println("isParticipant failed: "+e.getMessage()); return false; }
    }

    /** Completes an in-progress game. Null means no account-owned winner. */
    public static boolean completeGame(long gameId, Long winnerUserId) {
        if (gameId <= 0) return false;
        try {
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("status", "COMPLETED");
            body.put("winner_user_id", winnerUserId);
            body.put("ended_at", java.time.OffsetDateTime.now().toString());
            String path = "games?id=eq."+gameId+"&status=eq.IN_PROGRESS"
                    + "&select=status,winner_user_id";
            HttpRequest req = request(path).header("Content-Type","application/json")
                    .header("Prefer","return=representation")
                    .method("PATCH",HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
            HttpResponse<String> res = CLIENT.send(req,HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) { logFailure("completeGame",res); return false; }
            if (res.body() == null || res.body().isBlank() || "[]".equals(res.body().trim()))
                return isCompletionPersisted(gameId, winnerUserId);
            JsonNode rows = JSON.readTree(res.body());
            return rows.isArray() && !rows.isEmpty()
                    && "COMPLETED".equalsIgnoreCase(rows.get(0).path("status").asText())
                    && winnerMatches(rows.get(0).path("winner_user_id"), winnerUserId);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
          catch (Exception e) { System.err.println("completeGame failed for game="+gameId+": "+e.getMessage()); return false; }
    }

    /** Marks a game that ended without a completed match as abandoned. */
    public static boolean abandonGame(long gameId) {
        if (gameId <= 0) return false;
        try {
            Map<String, Object> body = Map.of(
                    "status", "ABANDONED",
                    "ended_at", java.time.OffsetDateTime.now().toString());
            String path = "games?id=eq." + gameId + "&status=eq.IN_PROGRESS&select=status";
            HttpRequest req = request(path).header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(
                            JSON.writeValueAsString(body))).build();
            HttpResponse<String> res = CLIENT.send(
                    req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                logFailure("abandonGame", res);
                return false;
            }
            if (res.body() == null || res.body().isBlank()
                    || "[]".equals(res.body().trim())) {
                return isGameStatus(gameId, "ABANDONED");
            }
            JsonNode rows = JSON.readTree(res.body());
            return rows.isArray() && !rows.isEmpty()
                    && "ABANDONED".equalsIgnoreCase(
                            rows.get(0).path("status").asText());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.err.println("abandonGame failed for game=" + gameId
                    + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns completed games for a player, including the opponent and
     * the requesting player's outcome.
     */
    public static ArrayNode getCompletedGamesForPlayer(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid player ID.");
        }
        try {
            Map<Long, Integer> playerNumbers = new HashMap<>();
            for (JsonNode playerGame : getAllRows(
                    "game_players?select=game_id,player_number&user_id=eq."
                            + userId + "&order=game_id.desc")) {
                long gameId = playerGame.path("game_id").asLong(-1);
                int playerNumber = playerGame.path("player_number").asInt(0);
                if (gameId > 0 && (playerNumber == 1 || playerNumber == 2)) {
                    playerNumbers.put(gameId, playerNumber);
                }
            }
            ArrayNode result = JSON.createArrayNode();
            if (playerNumbers.isEmpty()) {
                return result;
            }

            List<Long> gameIds = new ArrayList<>(playerNumbers.keySet());
            Map<Long, JsonNode> games = new LinkedHashMap<>();
            for (List<Long> chunk : chunks(gameIds, 100)) {
                String path = "games?select=id,started_at,variant_id,status,"
                        + "winner_user_id,room_code&status=eq.COMPLETED&id=in.("
                        + joinIds(chunk) + ")&order=started_at.desc";
                for (JsonNode game : getAllRows(path)) {
                    long id = game.path("id").asLong(-1);
                    if (id > 0 && "COMPLETED".equalsIgnoreCase(
                            game.path("status").asText())) {
                        games.put(id, game);
                    }
                }
            }
            if (games.isEmpty()) {
                return result;
            }

            Map<Long, List<JsonNode>> participants = new HashMap<>();
            List<Long> completedIds = new ArrayList<>(games.keySet());
            for (List<Long> chunk : chunks(completedIds, 100)) {
                String path = "game_players?select=game_id,user_id,player_number,"
                        + "users(username)&game_id=in.(" + joinIds(chunk) + ")";
                for (JsonNode participant : getAllRows(path)) {
                    long gameId = participant.path("game_id").asLong(-1);
                    participants.computeIfAbsent(gameId, ignored ->
                            new ArrayList<>()).add(participant);
                }
            }

            for (long gameId : completedIds) {
                JsonNode game = games.get(gameId);
                JsonNode opponent = findOpponent(
                        participants.getOrDefault(gameId, List.of()), userId);
                String opponentName = opponent.path("users")
                        .path("username").asText("");
                if (opponentName.isBlank()) {
                    String roomCode = game.path("room_code").asText("");
                    opponentName = roomCode.startsWith("A")
                            ? "AI" : "Local Player";
                }

                Object winnerValue = game.path("winner_user_id").isNull()
                        ? null : game.path("winner_user_id").asLong();
                String outcome;
                if (winnerValue instanceof Long winnerId) {
                    outcome = winnerId == userId ? "WIN" : "LOSS";
                } else {
                    String finalState = getFinalGameState(gameId);
                    if ("DRAW".equals(finalState)) {
                        outcome = "TIE";
                    } else if ("RED_WON".equals(finalState)
                            || "BLACK_WON".equals(finalState)) {
                        int winningPlayer = "RED_WON".equals(finalState) ? 1 : 2;
                        outcome = playerNumbers.get(gameId) == winningPlayer
                                ? "WIN" : "LOSS";
                    } else {
                        outcome = "TIE";
                    }
                }

                var entry = result.addObject();
                entry.put("gameId", gameId);
                if (game.path("started_at").isMissingNode()
                        || game.path("started_at").isNull()) {
                    entry.putNull("startedAt");
                } else {
                    entry.put("startedAt", game.path("started_at").asText());
                }
                entry.put("opponent", opponentName);
                entry.put("variant", game.path("variant_id").asText(""));
                entry.put("outcome", outcome);
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Game history lookup interrupted.");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not parse game history.", e);
        }
    }

    private static JsonNode findOpponent(List<JsonNode> participants, long userId) {
        for (JsonNode participant : participants) {
            if (participant.path("user_id").asLong(-1) != userId) {
                return participant;
            }
        }
        return JSON.nullNode();
    }

    private static String getFinalGameState(long gameId)
            throws InterruptedException, java.io.IOException {
        String path = "game_moves?select=move_data&game_id=eq." + gameId
                + "&order=move_number.desc&limit=1";
        JsonNode rows = getJson("getFinalGameState", path);
        return rows.isArray() && !rows.isEmpty()
                ? rows.get(0).path("move_data").path("gameState").asText("")
                : "";
    }

    private static boolean isGameStatus(long gameId, String expectedStatus)
            throws InterruptedException, java.io.IOException {
        HttpResponse<String> response = CLIENT.send(
                request("games?select=status&id=eq." + gameId + "&limit=1")
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logFailure("verifyGameStatus", response);
            return false;
        }
        JsonNode rows = JSON.readTree(response.body());
        return rows.isArray() && !rows.isEmpty()
                && expectedStatus.equalsIgnoreCase(rows.get(0).path("status").asText());
    }

    private static List<JsonNode> getAllRows(String path)
            throws InterruptedException, java.io.IOException {
        List<JsonNode> rows = new ArrayList<>();
        int offset = 0;
        while (true) {
            JsonNode page = getJson("getGameHistory",
                    path + "&limit=1000&offset=" + offset);
            if (!page.isArray()) {
                throw new IllegalStateException("Invalid game history response.");
            }
            for (JsonNode row : page) {
                rows.add(row);
            }
            if (page.size() < 1000) {
                return rows;
            }
            offset += page.size();
        }
    }

    private static JsonNode getJson(String operation, String path)
            throws InterruptedException, java.io.IOException {
        HttpResponse<String> response = CLIENT.send(
                request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logFailure(operation, response);
            throw new IllegalStateException("Could not retrieve game history.");
        }
        return JSON.readTree(response.body());
    }

    private static List<List<Long>> chunks(List<Long> values, int chunkSize) {
        List<List<Long>> chunks = new ArrayList<>();
        for (int start = 0; start < values.size(); start += chunkSize) {
            chunks.add(values.subList(start,
                    Math.min(start + chunkSize, values.size())));
        }
        return chunks;
    }

    private static String joinIds(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private static boolean isCompletionPersisted(long gameId, Long winnerUserId) throws Exception {
        String path = "games?select=status,winner_user_id&id=eq." + gameId + "&limit=1";
        HttpResponse<String> res = CLIENT.send(
                request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            logFailure("verifyGameCompletion", res);
            return false;
        }
        JsonNode rows = JSON.readTree(res.body());
        return rows.isArray() && !rows.isEmpty()
                && "COMPLETED".equalsIgnoreCase(rows.get(0).path("status").asText())
                && winnerMatches(rows.get(0).path("winner_user_id"), winnerUserId);
    }

    private static boolean winnerMatches(JsonNode storedWinner, Long winnerUserId) {
        return winnerUserId == null
                ? storedWinner.isNull()
                : storedWinner.canConvertToLong()
                        && storedWinner.asLong() == winnerUserId;
    }

    public static boolean isCompleted(long gameId) {
        try {
            HttpResponse<String> res = CLIENT.send(request("games?select=status&id=eq."+gameId+"&limit=1").GET().build(),HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) return false;
            JsonNode rows=JSON.readTree(res.body()); return rows.isArray()&&!rows.isEmpty()&&"COMPLETED".equalsIgnoreCase(rows.get(0).path("status").asText());
        } catch (Exception e) { if(e instanceof InterruptedException) Thread.currentThread().interrupt(); return false; }
    }

    public static JsonNode getGame(long gameId) {
        try {
            HttpResponse<String> res=CLIENT.send(request("games?select=id,status,winner_user_id,variant_id,started_at,ended_at&id=eq."+gameId+"&limit=1").GET().build(),HttpResponse.BodyHandlers.ofString());
            if(res.statusCode()<200||res.statusCode()>=300){logFailure("getGame",res);throw new IllegalStateException("Could not retrieve game metadata.");}
            JsonNode rows=JSON.readTree(res.body()); return rows.isArray()&&!rows.isEmpty()?rows.get(0):null;
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Game lookup interrupted.");}
          catch(java.io.IOException e){throw new IllegalStateException("Could not parse game metadata.",e);}
    }

    private static void logFailure(String operation,HttpResponse<String> response) {
        System.err.println("Supabase "+operation+" failed: HTTP "+response.statusCode()+"; response="+response.body());
    }
}
