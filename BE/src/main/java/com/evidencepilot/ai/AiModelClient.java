package com.evidencepilot.ai;

import com.evidencepilot.ai.dto.ClaimAnalysisRequest;
import com.evidencepilot.ai.dto.ClaimAnalysisResponse;
import com.evidencepilot.ai.dto.EmbeddingRequest;
import com.evidencepilot.ai.dto.EmbeddingResponse;
import com.evidencepilot.ai.dto.GenerateRequest;
import com.evidencepilot.ai.dto.GenerateResponse;
import com.evidencepilot.ai.dto.ModelsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Low-level HTTP client for the configured Python AI worker.
 *
 * <p>Java owns upload persistence, references, graph data, and orchestration.
 * The Python service is a stateless worker for extraction, embeddings,
 * generation, and claim analysis.</p>
 */
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

    // ── Liveness ───────────────────────────────────────────────────────────────

    /**
     * Calls {@code GET /health} and returns the raw response map.
     * Useful as a startup readiness check.
     */
        @SuppressWarnings("unchecked")
    public Map<String, Object> health() {
        log.debug("Calling GET /health");
        return get("/health", Map.class);
    }

// ── Models ─────────────────────────────────────────────────────────────────

    /**
     * Calls {@code GET /ai/models} and returns the list of available Ollama models.
     */
    public ModelsResponse listModels() {
        log.debug("Calling GET /ai/models");
        return get("/ai/models", ModelsResponse.class);
    }
    
    // ── Raw generation ─────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /ai/generate} with a raw prompt.
     *
     * @param prompt the prompt string (1–12 000 chars per Swagger)
     * @return the model's raw text response
     */
    public GenerateResponse generate(String prompt) {
        log.info("Calling POST /ai/generate (prompt length={})", prompt.length());
        return post("/ai/generate", new GenerateRequest(prompt), GenerateResponse.class);
    }

    // ── Claim analysis ─────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /process/claim} for a full AI analysis of a claim
     * against a specific source.
     *
     * <p>The response contains a {@code verdict}, {@code confidence} score,
     * and a detailed {@code explanation}. These are the primary outputs that
     * the service layer persists to the database.</p>
     *
     * @param request the analysis request with claim text, source_id, and excerpt
     * @return the AI verdict with confidence score and explanation
     */
    public ClaimAnalysisResponse processClaim(ClaimAnalysisRequest request) {
        log.info("Calling POST /process/claim (sourceId={}, claim length={})",
                request.sourceId(), request.claim().length());
        return post("/process/claim", request, ClaimAnalysisResponse.class);
    }

    // ── Paper review ───────────────────────────────────────────────────────────

    // ── Embeddings ─────────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /ai/embeddings} to generate a dense vector embedding
     * for the given text.
     *
     * <p>The returned list contains floating-point values representing the
     * text's position in the model's semantic space.  Typical dimensionality
     * is 384 (nomic-embed-text) or 1536 (text-embedding-3-small).</p>
     *
     * @param text the text to embed
     * @return the dense vector as a list of floats
     * @throws AiApiException if the AI service returns null/empty or a non-2xx status
     */
    public List<Float> generateEmbedding(String text) {
        log.info("Calling POST /ai/embeddings (text length={})", text.length());
        EmbeddingResponse response = post("/ai/embeddings",
                new EmbeddingRequest(text), EmbeddingResponse.class);
        if (response == null || response.embedding() == null || response.embedding().isEmpty()) {
            throw new AiApiException("/ai/embeddings", "returned null or empty embedding", null);
        }
        return response.embedding();
    }

    public ExtractDocumentResponse extractDocument(String filename, String contentType, byte[] raw) {
        log.info("Calling POST /extract (filename={}, bytes={})", filename, raw.length);

        ByteArrayResource resource = new ByteArrayResource(raw) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        if (contentType != null && !contentType.isBlank()) {
            fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, fileHeaders));

        return postMultipart("/extract", body, ExtractDocumentResponse.class);
    }

    private <T> T get(String endpoint, Class<T> responseType) {
        return call(endpoint, () -> restClient.get()
                .uri(baseUrl + endpoint)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw statusException("GET " + endpoint, res.getStatusCode().value());
                })
                .body(responseType));
    }

    private <T> T post(String endpoint, Object body, Class<T> responseType) {
        return call(endpoint, () -> restClient.post()
                .uri(baseUrl + endpoint)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw statusException("POST " + endpoint, res.getStatusCode().value());
                })
                .body(responseType));
    }

    private <T> T postMultipart(String endpoint, MultiValueMap<String, Object> body, Class<T> responseType) {
        return call(endpoint, () -> restClient.post()
                .uri(baseUrl + endpoint)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw statusException("POST " + endpoint, res.getStatusCode().value());
                })
                .body(responseType));
    }

    // ── Internal exception type ────────────────────────────────────────────────

    /**
     * Thrown when the AI API returns a non-2xx HTTP status.
     * Wraps the endpoint path and status code for clear error messages.
     */
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

    private AiApiException statusException(String operation, int statusCode) {
        return new AiApiException(operation, statusCode);
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

    public record ExtractDocumentResponse(String filename, String method, String markdown) {
    }

    /**
     * Thrown when the AI API returns a non-2xx status or the configured base URL is unavailable.
     */
    public static final class AiApiException extends RuntimeException {
        private final int statusCode;

        public AiApiException(String endpoint, int statusCode) {
            this(endpoint, statusCode, null);
        }

        public AiApiException(String endpoint, int statusCode, Throwable cause) {
            super("AI API error on " + endpoint + " - HTTP " + statusCode, cause);
            this.statusCode = statusCode;
        }

        public AiApiException(String endpoint, int statusCode, String message, Throwable cause) {
            super("AI API error on " + endpoint + " - " + message, cause);
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
