package com.evidencepilot.ai;

import com.evidencepilot.ai.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Low-level HTTP client for the teammate's Ollama Claim Analysis API (v0.2.0).
 *
 * <p>This component is intentionally thin — it owns only HTTP mechanics
 * (serialization, status-code handling, structured logging).
 * All orchestration and DB persistence live in
 * {@link com.evidencepilot.service.AiAnalysisService}.</p>
 *
 * <p><b>Active endpoints covered:</b>
 * <ul>
 *   <li>{@code GET  /health}           – liveness probe</li>
 *   <li>{@code GET  /ai/models}        – list available Ollama models</li>
 *   <li>{@code POST /ai/generate}      – raw prompt generation</li>
 *   <li>{@code POST /ai/embeddings}    – dense vector embedding generation</li>
 *   <li>{@code POST /match/claim}      – semantic source search for a claim</li>
 *   <li>{@code POST /process/claim}    – deep claim analysis against a known source</li>
 *   <li>{@code POST /review/paper}     – structural review of an uploaded paper</li>
 * </ul>
 * </p>
 *
 * <p><b>Excluded:</b> {@code POST /sources} and {@code POST /papers} — the Java
 * backend is the sole authority for physical file storage; those endpoints are
 * internal AI-team testing scaffolding only.</p>
 */
@Slf4j
@Component
public class AiModelClient {

    private final RestClient restClient;
    private final List<String> baseUrls;

    public AiModelClient(@Qualifier("aiRestClient") RestClient restClient,
                         @Qualifier("aiModelBaseUrls") List<String> baseUrls) {
        this.restClient = restClient;
        this.baseUrls = baseUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(AiModelClient::trimTrailingSlash)
                .distinct()
                .toList();

        if (this.baseUrls.isEmpty()) {
            throw new IllegalArgumentException("At least one AI model base URL must be configured");
        }
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

    // ── Claim matching ─────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /match/claim} to find sources semantically related to a claim.
     *
     * <p>Use the returned {@link ClaimMatchResponse#matches()} list to pick the best
     * {@code source_id} and {@code excerpt} before calling
     * {@link #processClaim(ClaimAnalysisRequest)}.</p>
     *
     * @param request the match request (use {@link ClaimMatchRequest#of(String)} for defaults)
     * @return ordered list of matching sources with suitability scores
     */
    public ClaimMatchResponse matchClaim(ClaimMatchRequest request) {
        log.info("Calling POST /match/claim (claim length={}, topK={})",
                request.claim().length(), request.topK());
        return post("/match/claim", request, ClaimMatchResponse.class);
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

    /**
     * Calls {@code POST /review/paper} to get a structural review of a paper
     * that the AI service already has registered under the given {@code paperId}.
     *
     * <p>Note: {@code paperId} here is an identifier inside the AI service's own
     * registry, not the {@code papers.id} primary key in our MySQL database.</p>
     *
     * @param request the review request
     * @return structural feedback including missing/weak sections and claim recommendations
     */
    public PaperReviewResponse reviewPaper(PaperReviewRequest request) {
        log.info("Calling POST /review/paper (paperId={}, targetStyle={}, useAi={})",
                request.paperId(), request.targetStyle(), request.useAi());
        return post("/review/paper", request, PaperReviewResponse.class);
    }

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

    private <T> T get(String endpoint, Class<T> responseType) {
        return callWithFallback(endpoint, absoluteUri -> restClient.get()
                .uri(absoluteUri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AiApiException("GET " + endpoint, res.getStatusCode().value());
                })
                .body(responseType));
    }

    private <T> T post(String endpoint, Object body, Class<T> responseType) {
        return callWithFallback(endpoint, absoluteUri -> restClient.post()
                .uri(absoluteUri)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AiApiException("POST " + endpoint, res.getStatusCode().value());
                })
                .body(responseType));
    }

    // ── Internal exception type ────────────────────────────────────────────────

    /**
     * Thrown when the AI API returns a non-2xx HTTP status.
     * Wraps the endpoint path and status code for clear error messages.
     */
    private <T> T callWithFallback(String endpoint, AiCall<T> call) {
        RuntimeException lastFailure = null;

        for (String baseUrl : baseUrls) {
            try {
                return call.execute(baseUrl + endpoint);
            } catch (AiApiException e) {
                if (!e.isRetriable()) {
                    throw e;
                }
                lastFailure = e;
                log.warn("AI endpoint {} failed at {} with HTTP {}. Trying next configured base URL.",
                        endpoint, baseUrl, e.getStatusCode());
            } catch (RestClientException e) {
                lastFailure = e;
                log.warn("AI endpoint {} failed at {}. Trying next configured base URL.",
                        endpoint, baseUrl, e);
            }
        }

        throw new AiApiException(endpoint, "all configured AI base URLs failed", lastFailure);
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
        T execute(String absoluteUri);
    }

    /**
     * Thrown when the AI API returns a non-2xx status or all configured base URLs fail.
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

        public AiApiException(String endpoint, String message, Throwable cause) {
            super("AI API error on " + endpoint + " - " + message, cause);
            this.statusCode = 0;
        }

        public int getStatusCode() {
            return statusCode;
        }

        private boolean isRetriable() {
            return statusCode == 0 || statusCode >= 500;
        }
    }
}
