package com.evidencepilot.client.qdrant;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantClientTest {

    @Test
    void upsertVectorUsesUuidPointId() throws IOException {
        AtomicReference<String> upsertBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/collections/source_chunks", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.createContext("/collections/source_chunks/points", exchange -> {
            upsertBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();

        try {
            UUID chunkId = UUID.randomUUID();
            QdrantClient client = new QdrantClient("http://127.0.0.1:" + server.getAddress().getPort());

            client.upsertVector(chunkId.toString(), List.of(0.1f, -0.2f), "PROJECT", UUID.randomUUID().toString());

            assertThat(upsertBody.get()).contains("\"id\":\"" + chunkId + "\"");
        } finally {
            server.stop(0);
        }
    }
}
