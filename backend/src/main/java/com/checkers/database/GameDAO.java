package com.checkers.database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GameDAO {

    public static long createGame(String roomCode) {

        try {
            String json = """
                    {
                        "room_code": "%s",
                        "variant": "CHECKERS",
                        "status": "IN_PROGRESS"
                    }
                    """.formatted(roomCode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.URL + "/rest/v1/games"
                    ))
                    .header("apikey", SupabaseConfig.API_KEY)
                    .header(
                            "Authorization",
                            "Bearer " + SupabaseConfig.API_KEY
                    )
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Create game status: " + response.statusCode()
            );

            System.out.println(
                    "Create game response: " + response.body()
            );

            if (response.statusCode() != 201) {
                return -1;
            }

            // Supabase returns something like:
            // [{"id":2,"room_code":"ABC123", ...}]

            String body = response.body();

            int idPosition = body.indexOf("\"id\":");

            if (idPosition == -1) {
                return -1;
            }

            int start = idPosition + 5;
            int end = start;

            while (end < body.length()
                    && Character.isDigit(body.charAt(end))) {
                end++;
            }

            return Long.parseLong(
                    body.substring(start, end)
            );

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}