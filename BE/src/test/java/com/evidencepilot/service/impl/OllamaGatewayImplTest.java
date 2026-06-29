package com.evidencepilot.service.impl;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaGatewayImplTest {

    @Test
    void embeddingCallUsesConfiguredReadTimeout() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ai/embeddings", exchange -> {
            try {
                Thread.sleep(Duration.ofSeconds(2).toMillis());
                byte[] body = "{\"embedding\":[0.1]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            OllamaGatewayImpl gateway = new OllamaGatewayImpl(baseUrl, "model", "model", 1);

            long started = System.nanoTime();
            assertThatThrownBy(() -> gateway.getDenseEmbedding("slow chunk"))
                    .isInstanceOf(RuntimeException.class);

            long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            assertThat(elapsedMillis).isLessThan(Duration.ofSeconds(2).toMillis());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void emptyEmbeddingResponseFailsInsteadOfSkippingChunk() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ai/embeddings", exchange -> {
            byte[] body = "{\"embedding\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            OllamaGatewayImpl gateway = new OllamaGatewayImpl(baseUrl, "model", "model", 1);

            assertThatThrownBy(() -> gateway.getDenseEmbedding("chunk"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Embedding response was null or empty");
        } finally {
            server.stop(0);
        }
    }
}
