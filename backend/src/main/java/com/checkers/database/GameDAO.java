package com.checkers.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
