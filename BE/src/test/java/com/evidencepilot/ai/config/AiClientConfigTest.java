package com.evidencepilot.ai.config;

import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.impl.AiModelClientImpl;
import com.evidencepilot.client.ai.config.AiClientConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("Temporarily disabled pending post-AI pipeline refactor")
class AiClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiClientConfig.class);

    @Test
    void aiModelBaseUrlUsesOnlyConfiguredEnvironmentValue() {
        contextRunner
                .withPropertyValues(
                        "ai.model.local-base-url=http://127.0.0.1:8000",
                        "ai.model.ngrok-base-url=https://good-lumpish-headstone.ngrok-free.dev",
                        "ai.model.base-url=https://configured-ai.example.test")
                .run(context -> {
                    assertThat(context.getBean("aiModelBaseUrl", String.class))
                            .isEqualTo("https://configured-ai.example.test");
                    assertThat(context.containsBean("aiModelBaseUrls")).isFalse();
                });
    }

    @Test
    void appliesConfiguredReadTimeoutToAiCalls() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            try {
                Thread.sleep(Duration.ofSeconds(2).toMillis());
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
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
            new ApplicationContextRunner()
                    .withUserConfiguration(AiClientConfig.class)
                    .withPropertyValues(
                            "ai.model.base-url=" + baseUrl,
                            "ai.model.api-key=",
                            "ai.model.read-timeout-seconds=1")
                    .run(context -> {
                        AiModelClientImpl client = new AiModelClientImpl(
                                context.getBean("aiRestClient", RestClient.class),
                                context.getBean("aiModelBaseUrl", String.class));

                        long started = System.nanoTime();
                        assertThatThrownBy(client::health)
                                .isInstanceOf(AiModelClient.AiApiException.class)
                                .hasMessageContaining("/health");

                        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
                        assertThat(elapsedMillis).isLessThan(Duration.ofSeconds(2).toMillis());
                    });
        } finally {
            server.stop(0);
        }
    }
}
