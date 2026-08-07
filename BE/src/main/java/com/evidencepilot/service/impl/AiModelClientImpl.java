package com.evidencepilot.service.impl;

import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.ExtractionBundle;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiModelClientImpl implements AiModelClient {

    private static final MediaType APPLICATION_ZIP = MediaType.valueOf("application/zip");

    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public AiModelClientImpl(@Qualifier("aiRestClient") RestClient restClient,
            @Qualifier("aiModelBaseUrl") String baseUrl,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "" : trimTrailingSlash(baseUrl);
        this.objectMapper = objectMapper;
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
    public GenerationResult generate(String system, String prompt) {
        Map<String, Object> response = call("/ai/generate", () -> restClient.post()
                .uri(baseUrl + "/ai/generate")
                .body(Map.of(
                        "system", system == null ? "" : system,
                        "prompt", prompt))
                .retrieve()
                .body(Map.class));
        if (response == null
                || !hasText(response.get("provider"))
                || !hasText(response.get("model"))
                || !hasText(response.get("response"))) {
            throw new AiApiException("/ai/generate", "returned null or empty response", null);
        }
        return new GenerationResult(
                String.valueOf(response.get("provider")),
                String.valueOf(response.get("model")),
                String.valueOf(response.get("response")));
    }

    @Override
    public ExtractionBundle extractDocument(String filename, String downloadUrl) {
        Path archivePath;
        try {
            archivePath = Files.createTempFile("evidencepilot-extraction-", ".zip");
        } catch (IOException e) {
            throw new AiApiException("/extract", "could not create temporary archive", e);
        }

        boolean returned = false;
        try {
            ExtractionBundle bundle = call("/extract", () -> restClient.post()
                    .uri(baseUrl + "/extract")
                    .accept(APPLICATION_ZIP)
                    .body(Map.of(
                            "filename", stringValue(filename, "document"),
                            "download_url", downloadUrl))
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new AiApiException("/extract", response.getStatusCode().value());
                        }
                        if (!APPLICATION_ZIP.equalsTypeAndSubtype(response.getHeaders().getContentType())) {
                            throw new AiApiException("/extract", "did not return application/zip", null);
                        }
                        try (InputStream input = response.getBody();
                                OutputStream output = Files.newOutputStream(archivePath)) {
                            if (input == null) {
                                throw new IOException("Extraction bundle response body is empty");
                            }
                            copyWithLimit(input, output, 100L * 1024 * 1024);
                        } catch (IOException e) {
                            throw new AiApiException("/extract", "could not download extraction bundle", e);
                        }
                        try {
                            return ExtractionBundle.open(archivePath);
                        } catch (IOException e) {
                            throw new AiApiException("/extract", "returned an invalid extraction bundle", e);
                        }
                    }));
            returned = true;
            return bundle;
        } finally {
            if (!returned) {
                try {
                    Files.deleteIfExists(archivePath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void copyWithLimit(InputStream input, OutputStream output, long maxBytes) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Extraction bundle exceeds the 100 MiB limit");
            }
            output.write(buffer, 0, read);
        }
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
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("AI endpoint {} returned HTTP {} at configured base URL {}.",
                    endpoint, status, baseUrl);
            throw new AiApiException(endpoint, status);
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

    private static boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    @FunctionalInterface
    private interface AiCall<T> {
        T execute();
    }
}
