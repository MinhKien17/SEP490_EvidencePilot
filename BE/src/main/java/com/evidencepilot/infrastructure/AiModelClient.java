package com.evidencepilot.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AiModelClient {

    private final RestClient restClient;
    private final String baseUrl;

    public AiModelClient(@Qualifier("aiRestClient") RestClient restClient,
            @Qualifier("aiModelBaseUrl") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "" : trimTrailingSlash(baseUrl);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> health() {
        return call("/health", () -> restClient.get()
                .uri(baseUrl + "/health")
                .retrieve()
                .body(Map.class));
    }

    public String generate(String prompt) {
        return call("/ai/generate", () -> restClient.post()
                .uri(baseUrl + "/ai/generate")
                .body(Map.of("prompt", prompt))
                .retrieve()
                .body(String.class));
    }

    public Map<String, Object> matchClaim(UUID claimId, String claimText, int topK) {
        return call("/match/claim", () -> restClient.post()
                .uri(baseUrl + "/match/claim")
                .body(Map.of("claim_id", claimId.toString(), "claim", claimText, "top_k", topK))
                .retrieve()
                .body(Map.class));
    }

    public Map<String, Object> processClaim(UUID claimId, String claimText, UUID sourceId, String excerpt) {
        return call("/process/claim", () -> restClient.post()
                .uri(baseUrl + "/process/claim")
                .body(Map.of("claim_id", claimId.toString(), "claim", claimText,
                        "source_id", sourceId.toString(), "excerpt", excerpt))
                .retrieve()
                .body(Map.class));
    }

    public Map<String, Object> reviewPaper(UUID paperId, String targetStyle, boolean useAi) {
        return call("/review/paper", () -> restClient.post()
                .uri(baseUrl + "/review/paper")
                .body(Map.of("paper_id", paperId.toString(), "target_style", targetStyle, "use_ai", useAi))
                .retrieve()
                .body(Map.class));
    }

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

    public void indexChunk(UUID chunkId, UUID documentId, String text) {
        log.info("Calling POST /index/chunk (chunkId={}, documentId={})", chunkId, documentId);
        call("/index/chunk", () -> restClient.post()
                .uri(baseUrl + "/index/chunk")
                .body(Map.of("chunk_id", chunkId.toString(), "document_id", documentId.toString(), "text", text))
                .retrieve()
                .toBodilessEntity());
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

    public static final class AiApiException extends RuntimeException {
        private final int statusCode;

        public AiApiException(String endpoint, int statusCode) {
            this(endpoint, statusCode, null, null);
        }

        public AiApiException(String endpoint, int statusCode, Throwable cause) {
            this(endpoint, statusCode, null, cause);
        }

        public AiApiException(String endpoint, int statusCode, String message, Throwable cause) {
            super("AI API error on " + endpoint + " - HTTP " + statusCode + (message != null ? " " + message : ""),
                    cause);
            this.statusCode = statusCode;
        }

        public AiApiException(String endpoint, String message, Throwable cause) {
            super("AI API error on " + endpoint + " - " + message, cause);
            this.statusCode = 0;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
