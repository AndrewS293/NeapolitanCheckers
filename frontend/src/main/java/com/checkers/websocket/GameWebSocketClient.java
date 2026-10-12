package com.checkers.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class GameWebSocketClient implements WebSocket.Listener {

    private static final String WS_URL =
            "wss://neapolitancheckers.onrender.com/online";

    private final ObjectMapper mapper = new ObjectMapper();
    private volatile Consumer<JsonNode> messageHandler;
    private final Consumer<String> statusHandler;
    private final HttpClient httpClient;

    private WebSocket webSocket;
    private StringBuilder incoming = new StringBuilder();

    public GameWebSocketClient(
            HttpClient httpClient,
            Consumer<JsonNode> messageHandler,
            Consumer<String> statusHandler) {

        this.httpClient = httpClient;
        this.messageHandler = messageHandler;
        this.statusHandler = statusHandler;
    }

    public void connect(String gameId, String username) {
        try {
            URI uri = URI.create(
                    WS_URL
                    + "?gameId=" + encode(gameId)
                    + "&username=" + encode(username)
            );

            WebSocket.Builder builder = httpClient.newWebSocketBuilder();

            // Forward the authenticated HTTP session cookie.
            httpClient.cookieHandler().ifPresent(handler -> {
                if (handler instanceof CookieManager cookieManager) {
                    var cookies = cookieManager.getCookieStore()
                            .get(uri);

                    if (!cookies.isEmpty()) {
                        String cookieHeader = String.join(
                                "; ",
                                cookies.stream()
                                        .map(c -> c.getName() + "=" + c.getValue())
                                        .toList()
                        );

                        builder.header("Cookie", cookieHeader);
                    }
                }
            });

            statusHandler.accept("Connecting to game " + gameId + "...");

            builder.buildAsync(uri, this)
                    .whenComplete((socket, error) -> {
                        if (error != null) {
                            statusHandler.accept(
                                    "Connection failed: " + error.getMessage()
                            );
                        } else {
                            webSocket = socket;
                            statusHandler.accept("WebSocket connected.");
                        }
                    });

        } catch (Exception e) {
            statusHandler.accept("Connection error: " + e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        statusHandler.accept("Connected to game server.");
        webSocket.request(1);
    }

    
    @Override
    public CompletionStage<?> onText(
            WebSocket webSocket,
            CharSequence data,
            boolean last) {

        System.out.println(
            "WEBSOCKET RAW MESSAGE: " + data
            + " | last=" + last
        );

        incoming.append(data);

        if (last) {
            try {
                String rawMessage = incoming.toString();
                JsonNode message = mapper.readTree(rawMessage);

                System.out.println(
                    "WEBSOCKET PARSED MESSAGE: " + message
                );

                Consumer<JsonNode> handler = messageHandler;

                if (handler == null) {
                    System.err.println(
                        "No message handler set; message not processed."
                    );
                } else {
                    handler.accept(message);
                }

            } catch (Exception e) {
                System.err.println(
                    "Failed to process WebSocket message."
                );
                e.printStackTrace();

            } finally {
                incoming.setLength(0);
            }
        }

        webSocket.request(1);
        return null;
    }


    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        statusHandler.accept("WebSocket error: " + error.getMessage());
    }

    @Override
    public CompletionStage<?> onClose(
            WebSocket webSocket,
            int statusCode,
            String reason) {

        statusHandler.accept("Disconnected: " + reason);
        return null;
    }

    public void send(String json) {
        WebSocket current = webSocket;

        if (current != null) {
            current.sendText(json, true);
        } else {
            statusHandler.accept("Cannot send: not connected.");
        }
    }

    
    public void sendMove(
            String gameId,
            int fromRow,
            int fromCol,
            int toRow,
            int toCol) {

        try {
            ObjectMapper mapper = new ObjectMapper();

            com.fasterxml.jackson.databind.node.ObjectNode move =
                    mapper.createObjectNode();

            move.put("fromRow", fromRow);
            move.put("fromCol", fromCol);
            move.put("toRow", toRow);
            move.put("toCol", toCol);

            com.fasterxml.jackson.databind.node.ObjectNode message =
                    mapper.createObjectNode();

            message.put("type", "MOVE");
            message.put("gameId", gameId);
            message.set("message", move);

            send(mapper.writeValueAsString(message));

        } catch (Exception e) {
            System.err.println(
                    "Could not send move: " + e.getMessage()
            );
        }
    }


    public void disconnect() {
        WebSocket current = webSocket;

        if (current != null) {
            current.sendClose(
                    WebSocket.NORMAL_CLOSURE,
                    "Player left game"
            );
            webSocket = null;
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(
                value,
                java.nio.charset.StandardCharsets.UTF_8
        );
    }

    public void setMessageHandler(Consumer<JsonNode> handler) {
        this.messageHandler = handler;
    }
}
