package com.checkers.database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// implements movedao component of application
public class MoveDAO {
<<<<<<< HEAD
// defines the method to save a move to the database
=======

    //Saves a move within our Supabase PostgreSQL database
>>>>>>> c64f96214cc677383dc7dea08ba4818d633b2e37
    public static void saveMove(
            long gameId,
            long playerId,
            int moveNumber,
            String from,
            String to) {

        try {

            String notation = from + "-" + to;

            String json = """
                    {
                        "game_id": %d,
                        "move_number": %d,
                        "player_id": %d,
                        "move_data": {
                            "notation": "%s",
                            "from": "%s",
                            "to": "%s",
                            "capture": false,
                            "king": false
                        }
                    }
                    """.formatted(
                        gameId,
                        moveNumber,
                        playerId,
                        notation,
                        from,
                        to
                    );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                        SupabaseConfig.URL + "/rest/v1/game_moves"
                    ))
                    .header("apikey", SupabaseConfig.API_KEY)
                    .header(
                        "Authorization",
                        "Bearer " + SupabaseConfig.API_KEY
                    )
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                "Status: " + response.statusCode()
            );

            System.out.println(
                "Response: " + response.body()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}