package com.evidencepilot.service.impl;

import com.evidencepilot.service.AiModelClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

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

    @Override
    public Map<String, Object> processClaim(UUID claimId, String claimText, UUID sourceId, String excerpt) {
        return call("/process/claim", () -> restClient.post()
                .uri(baseUrl + "/process/claim")
                .body(Map.of("claim_id", claimId.toString(), "claim", claimText,
                        "source_id", sourceId.toString(), "excerpt", excerpt))
                .retrieve()
                .body(Map.class));
    }

    @Override
    public double[] generateEmbedding(String text) {
        Map<String, Object> response = call("/ai/embeddings", () -> restClient.post()
                .uri(baseUrl + "/ai/embeddings")
                .body(Map.of("text", text))
                .retrieve()
                .body(Map.class));
        if (response == null || !response.containsKey("embedding")) {
            throw new AiApiException("/ai/embeddings", "returned null or empty embedding", null);
        }
        @SuppressWarnings("unchecked")
        var list = (java.util.List<Number>) response.get("embedding");
        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).doubleValue();
        }
        return result;
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

    @FunctionalInterface
    private interface AiCall<T> {
        T execute();
    }
}
