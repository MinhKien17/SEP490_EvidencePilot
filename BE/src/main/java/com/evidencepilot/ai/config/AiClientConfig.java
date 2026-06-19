package com.evidencepilot.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Spring configuration that creates a pre-configured {@link RestClient} bean
 * pointing at the teammate's AI model base URL.
 *
 * <p>Configuration keys (all overridable via environment variables):
 * <ul>
 *   <li>{@code ai.model.base-url}  / {@code AI_MODEL_BASE_URL} – required</li>
 *   <li>{@code ai.model.api-key}   / {@code AI_MODEL_API_KEY}  – optional, sent as {@code X-API-Key}</li>
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

    @Value("${ai.model.fallback-base-url:}")
    private String fallbackBaseUrl;

    @Value("${ai.model.api-key:}")
    private String apiKey;

    @Bean("aiModelBaseUrls")
    public List<String> aiModelBaseUrls() {
        return List.of(localBaseUrl, ngrokBaseUrl, fallbackBaseUrl);
    }

    /**
     * Named bean {@code aiRestClient} injected into {@link com.evidencepilot.ai.AiModelClient}.
     */
    @Bean("aiRestClient")
    public RestClient aiRestClient() {
        RestClient.Builder builder = RestClient.builder()
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
