package com.evidencepilot.service.impl;

import com.evidencepilot.service.AiModelClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiModelClientImpl implements AiModelClient {

    private final RestClient restClient;
    private final String baseUrl;

    public AiModelClientImpl(@Qualifier("aiRestClient") RestClient restClient,
            @Qualifier("aiModelBaseUrl") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "" : trimTrailingSlash(baseUrl);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> health() {
        return call("/health", () -> restClient.get()
                .uri(baseUrl + "/health")
                .retrieve()
                .body(Map.class));
    }

    @Override
    public String generate(String prompt) {
        Map<String, Object> response = call("/ai/generate", () -> restClient.post()
                .uri(baseUrl + "/ai/generate")
                .body(Map.of("prompt", prompt))
                .retrieve()
                .body(Map.class));
        if (response == null || response.get("response") == null) {
            throw new AiApiException("/ai/generate", "returned null or empty response", null);
        }
        return String.valueOf(response.get("response"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ExtractedDocument extractDocument(String filename, String downloadUrl) {
        Map<String, Object> response = call("/extract", () -> restClient.post()
                .uri(baseUrl + "/extract")
                .body(Map.of(
                        "filename", stringValue(filename, "document"),
                        "download_url", downloadUrl))
                .retrieve()
                .body(Map.class));

        if (response == null || response.get("markdown") == null) {
            throw new AiApiException("/extract", "returned null or empty markdown", null);
        }
        return new ExtractedDocument(
                stringValue(response.get("filename"), filename),
                stringValue(response.get("method"), "unknown"),
                stringValue(response.get("markdown"), ""));
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        Map<String, Object> response = call("/ai/embeddings", () -> restClient.post()
                .uri(baseUrl + "/ai/embeddings")
                .body(Map.of("text", text))
                .retrieve()
                .body(Map.class));
        if (response == null || !response.containsKey("embedding")) {
            throw new AiApiException("/ai/embeddings", "returned null or empty embedding", null);
        }
        return floatVector(response.get("embedding"), "/ai/embeddings");
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        Map<String, Object> response = call("/ai/embeddings/batch", () -> restClient.post()
                .uri(baseUrl + "/ai/embeddings/batch")
                .body(Map.of("texts", texts))
                .retrieve()
                .body(Map.class));
        if (response == null || !(response.get("embeddings") instanceof List<?> raw)
                || raw.size() != texts.size()) {
            throw new AiApiException("/ai/embeddings/batch", "returned an invalid embedding count", null);
        }
        return raw.stream()
                .map(vector -> floatVector(vector, "/ai/embeddings/batch"))
                .toList();
    }

    private <T> T call(String endpoint, AiCall<T> call) {
        if (baseUrl.isBlank()) {
            throw new AiApiException(endpoint, 503, "AI_MODEL_BASE_URL is not configured", null);
        }
        try {
            return call.execute();
        } catch (AiApiException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("AI endpoint {} failed at configured base URL {}.", endpoint, baseUrl, e);
            throw new AiApiException(endpoint, 503, "AI model offline at " + baseUrl, e);
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static List<Float> floatVector(Object raw, String endpoint) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new AiApiException(endpoint, "returned an empty embedding", null);
        }
        try {
            return list.stream()
                    .map(value -> ((Number) value).floatValue())
                    .toList();
        } catch (ClassCastException e) {
            throw new AiApiException(endpoint, "returned a non-numeric embedding", e);
        }
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return String.valueOf(value);
    }

    @FunctionalInterface
    private interface AiCall<T> {
        T execute();
    }
}
