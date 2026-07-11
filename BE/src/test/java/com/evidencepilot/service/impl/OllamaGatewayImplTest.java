package com.evidencepilot.service.impl;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.service.AiModelClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaGatewayImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void embeddingUsesRabbitRequestReply() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        Message reply = MessageBuilder.withBody("{\"embedding\":[0.1,-0.2]}".getBytes()).build();
        when(rabbit.sendAndReceive(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY_EMBEDDING_REQUEST),
                any(Message.class))).thenReturn(reply);

        OllamaGatewayImpl gateway = new OllamaGatewayImpl(
                rabbit, objectMapper, "http://unused", "nomic-embed-text", "model", 2);

        assertThat(gateway.getDenseEmbedding("claim")).isEqualTo(List.of(0.1f, -0.2f));
    }

    @Test
    void embeddingTimeoutFailsInsteadOfReturningEmptyVector() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        OllamaGatewayImpl gateway = new OllamaGatewayImpl(
                rabbit, objectMapper, "http://unused", "model", "model", 1);

        assertThatThrownBy(() -> gateway.getDenseEmbedding("claim"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void generateTimeoutStillUsesPythonHttpService() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ai/generate", exchange -> {
            try {
                Thread.sleep(Duration.ofSeconds(2).toMillis());
                byte[] body = "{\"response\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
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
            RabbitTemplate rabbit = mock(RabbitTemplate.class);
            OllamaGatewayImpl gateway = new OllamaGatewayImpl(
                    rabbit,
                    objectMapper,
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "model",
                    "model",
                    1);
            assertThatThrownBy(() -> gateway.generateEvaluation("slow prompt"))
                    .isInstanceOf(AiModelClient.AiApiException.class)
                    .hasMessageContaining("/ai/generate");
        } finally {
            server.stop(0);
        }
    }
}
