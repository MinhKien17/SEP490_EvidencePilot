package com.evidencepilot.service.impl;

import com.evidencepilot.service.AiModelClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    @SuppressWarnings("unchecked")
    @Override
    public ExtractedDocument extractDocument(String filename, String contentType, byte[] content) {
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename == null || filename.isBlank() ? "document" : filename;
            }
        };

        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(parseMediaType(contentType));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, partHeaders));

        Map<String, Object> response = call("/extract", () -> restClient.post()
                .uri(baseUrl + "/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
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

    private static MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
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
