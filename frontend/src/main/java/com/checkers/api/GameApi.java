package com.checkers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GameApi {

        //sends info to our gamecontroller to create/join/get games

    private static final String BASE_URL =
        "https://neapolitancheckers.onrender.com/api/games";;

    private final HttpClient client;
    private final ObjectMapper mapper;

    public GameApi(AuthApi authApi) {
        this.client = authApi.getClient();
        this.mapper = new ObjectMapper();
    }

    public JsonNode createGame(String visibility, String variantId)
        throws Exception {

        String url = BASE_URL
                + "?visibility=" + visibility
                + "&variantId=" + variantId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json")
                .build();

        return send(request);
    }

    public JsonNode getOpenGames()
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/open"))
                .GET()
                .header("Accept", "application/json")
                .build();

        return send(request);
    }

    public JsonNode joinPublicGame(String gameId)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        BASE_URL + "/" + gameId + "/join"
                ))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json")
                .build();

        JsonNode response = send(request);

        System.out.println("YOOOO JOIN PUB GAME RESPONSE: " + response);

        return send(request);
    }

    public JsonNode joinPrivateGame(String joinCode)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        BASE_URL + "/join/" + joinCode
                ))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json")
                .build();

        return send(request);
    }

    public JsonNode getGame(String gameId)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        BASE_URL + "/" + gameId
                ))
                .GET()
                .header("Accept", "application/json")
                .build();

        return send(request);
    }
    // Retrieves all saved moves for a game.
    public JsonNode getReplay(long databaseGameId)
            throws Exception {

        if (databaseGameId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid database game ID."
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        BASE_URL + "/" + databaseGameId + "/replay"
                ))
                .GET()
                .header("Accept", "application/json")
                .build();

        return send(request);
    }

    // Retrieves the board after a selected number of moves.
    public JsonNode getReplayPosition(
            long databaseGameId,
            int position) throws Exception {

        if (databaseGameId <= 0 || position < 0) {
            throw new IllegalArgumentException(
                    "Invalid game ID or replay position."
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        BASE_URL + "/" + databaseGameId
                                + "/replay?position=" + position
                ))
                .GET()
                .header("Accept", "application/json")
                .build();

        return send(request);
    }
    private JsonNode send(HttpRequest request)
            throws Exception {

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonNode body = mapper.readTree(response.body());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            JsonNode error = body == null
                    ? null
                    : body.get("error");

            String message =
                    error != null
                            ? error.asText()
                            : "Game request failed with HTTP "
                                    + response.statusCode();

            throw new Exception(message);
        }

        return body;
    }
}