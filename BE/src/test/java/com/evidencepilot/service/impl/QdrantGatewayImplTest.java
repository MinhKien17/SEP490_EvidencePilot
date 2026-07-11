package com.evidencepilot.service.impl;

import com.evidencepilot.dto.SparseVector;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantGatewayImplTest {

    @Test
    void searchDocumentContext_sendsScopedHybridQueryAndReturnsNonBlankTexts() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            send(exchange, 200, """
                    {"result":{"points":[
                      {"id":"1","score":0.8,"payload":{"text":"first chunk"}},
                      {"id":"2","score":0.5,"payload":{"text":" "}},
                      {"id":"3","score":0.0,"payload":{"text":"ignored"}}
                    ]}}
                    """);
        });
        try {
            UUID documentId = UUID.randomUUID();
            var gateway = new QdrantGatewayImpl(url(server), "secret");

            assertThat(gateway.searchDocumentContext(
                    documentId, List.of(0.2f), new SparseVector(List.of(1L), List.of(1f)), 3))
                    .containsExactly("first chunk");
            assertThat(body.get()).contains(documentId.toString(), "\"limit\":3");
            assertThat(apiKey.get()).isEqualTo("secret");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void searchDocumentContext_returnsEmptyForNullResult() throws Exception {
        HttpServer server = server(exchange -> send(exchange, 200, "{}"));
        try {
            var gateway = new QdrantGatewayImpl(url(server), "");
            assertThat(gateway.searchDocumentContext(
                    UUID.randomUUID(), List.of(), new SparseVector(List.of(), List.of()), 1)).isEmpty();
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer server(Handler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/collections/source_chunks/points/query", exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    private static String url(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
