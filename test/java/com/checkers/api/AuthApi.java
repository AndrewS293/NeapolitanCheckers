package com.checkers.api;

import com.checkers.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthApi {

    private static final String BASE_URL =
            "http://localhost:8080/api/auth";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public AuthApi() {

        CookieManager cookieManager =
                new CookieManager();

        cookieManager.setCookiePolicy(
                CookiePolicy.ACCEPT_ALL
        );

        client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();

        mapper = new ObjectMapper();
    }

    public User login(
            String username,
            String password)
            throws Exception {

        Map<String, String> data = new HashMap<>();

        data.put("username", username);
        data.put("password", password);

        String json =
                mapper.writeValueAsString(data);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        BASE_URL + "/login"
                                )
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new Exception(
                    extractErrorMessage(
                            response.body(),
                            "Login failed."
                    )
            );
        }

        return mapper.readValue(
                response.body(),
                User.class
        );
    }


    public User register(
            String username,
            String email,
            String password)
            throws Exception {

        Map<String, String> data = new HashMap<>();

        data.put("username", username);
        data.put("email", email);
        data.put("password", password);

        String json =
                mapper.writeValueAsString(data);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        BASE_URL + "/register"
                                )
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new Exception(
                    extractErrorMessage(
                            response.body(),
                            "Registration failed."
                    )
            );
        }

        return mapper.readValue(
                response.body(),
                User.class
        );
    }


    public void logout() throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        BASE_URL + "/logout"
                                )
                        )
                        .POST(
                                HttpRequest.BodyPublishers.noBody()
                        )
                        .build();

        client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }


    /*
     * Extracts the "error" field returned by Spring Boot.
     *
     * Expected response:
     *
     * {
     *     "error": "Username already exists."
     * }
     */
    private String extractErrorMessage(
            String responseBody,
            String fallbackMessage) {

        try {

            JsonNode node =
                    mapper.readTree(responseBody);

            JsonNode error =
                    node.get("error");

            if (error != null &&
                    !error.asText().isBlank()) {

                return error.asText();
            }

        } catch (Exception ignored) {
            // Fall back to the default message.
        }

        return fallbackMessage;
    }
}

