package com.evidencepilot.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Spring configuration that creates a pre-configured {@link RestClient} bean
 * pointing at the teammate's AI model base URL.
 *
 * <p>Configuration keys (all overridable via environment variables):
 * <ul>
 *   <li>{@code ai.model.local-base-url} / {@code AI_MODEL_LOCAL_BASE_URL} - first AI worker URL</li>
 *   <li>{@code ai.model.ngrok-base-url} / {@code AI_MODEL_NGROK_BASE_URL} - second AI worker URL</li>
 *   <li>{@code ai.model.base-url}       / {@code AI_MODEL_BASE_URL}       - fallback AI worker URL</li>
 *   <li>{@code ai.model.api-key}        / {@code AI_MODEL_API_KEY}        - optional, sent as {@code X-API-Key}</li>
 * </ul>
 * </p>
 *
 * <p><b>ngrok compatibility:</b> The header {@code ngrok-skip-browser-warning: true}
 * is always added.  It is harmless on non-ngrok servers and required for ngrok-hosted
 * APIs to bypass the browser interstitial page that would otherwise corrupt JSON
 * responses when called programmatically.</p>
 */
@Configuration
public class AiClientConfig {

    @Value("${ai.model.local-base-url}")
    private String localBaseUrl;

    @Value("${ai.model.ngrok-base-url}")
    private String ngrokBaseUrl;

    @Value("${ai.model.base-url}")
    private String baseUrl;

    @Value("${ai.model.api-key:}")
    private String apiKey;

    @Bean("aiModelBaseUrl")
    public String aiModelBaseUrl() {
        return baseUrl;
    }

    @Bean("aiModelBaseUrls")
    public List<String> aiModelBaseUrls() {
        return List.of(localBaseUrl, ngrokBaseUrl, baseUrl);
    }

    /**
     * Named bean {@code aiRestClient} injected into {@link com.evidencepilot.ai.AiModelClient}.
     */
    @Bean("aiRestClient")
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // Bypass ngrok browser-interstitial for any ngrok-hosted AI endpoint
                .defaultHeader("ngrok-skip-browser-warning", "true");

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }

        return builder.build();
    }
}
