package com.evidencepilot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin REST client for the Qdrant vector database.
 *
 * <p>Handles two operations:
 * <ol>
 *   <li>{@link #upsertVector(String, List, String)} – writes a point into the
 *       {@code source_chunks} collection with the MySQL chunk ID and a
 *       {@code project_id} payload for filtered search.</li>
 *   <li>{@link #findClosestChunkId(List, String)} – performs a nearest-neighbour
 *       search filtered by {@code project_id} and returns the MySQL chunk ID of
 *       the top result.</li>
 * </ol>
 *
 * <p>The collection is auto-created on the first upsert if it does not already
 * exist.  This avoids external setup steps and keeps the Docker Compose
 * experience zero-config.</p>
 *
 * <p><b>Configuration:</b> requires {@code QDRANT_URL} environment variable
 * (mapped via {@code qdrant.url} in {@code application.yml}).</p>
 */
@Slf4j
@Service
public class QdrantClient {

    private static final String COLLECTION = "source_chunks";

    private final RestClient restClient;
    private final String baseUrl;

    /** Tracks whether we have already ensured the collection exists in this JVM lifetime. */
    private volatile boolean collectionEnsured = false;

    public QdrantClient(@Value("${qdrant.url:http://localhost:6333}") String qdrantUrl) {
        this.baseUrl = trimTrailingSlash(qdrantUrl);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("QdrantClient initialized – base URL: {}", this.baseUrl);
    }

    // ── Write ──────────────────────────────────────────────────────────────────

    /**
     * Upserts a single point into the {@code source_chunks} collection.
     *
     * <p>The Qdrant point uses the MySQL chunk ID as a numeric point ID.
     * A {@code project_id} payload is attached so that reads can be filtered
     * per project.</p>
     *
     * @param chunkId   the MySQL {@code source_chunks.id} (must be numeric)
     * @param embedding the dense vector produced by the embedding model
     * @param projectId the MySQL {@code projects.id} (stored as payload for filtering)
     */
    public void upsertVector(String chunkId, List<Float> embedding, String projectId) {
        ensureCollection(embedding.size());

        Map<String, Object> point = Map.of(
                "id", Long.parseLong(chunkId),
                "vector", embedding,
                "payload", Map.of("project_id", projectId)
        );

        Map<String, Object> body = Map.of("points", List.of(point));
        String url = baseUrl + "/collections/" + COLLECTION + "/points?wait=true";

        try {
            restClient.put()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        throw new QdrantException("Failed to sync vector to Qdrant");
                    })
                    .toBodilessEntity();
            log.debug("Upserted chunkId={} into Qdrant (projectId={})", chunkId, projectId);
        } catch (RestClientException e) {
            throw new QdrantException("Failed to sync vector to Qdrant", e);
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    /**
     * Finds the MySQL chunk ID of the closest vector in the {@code source_chunks}
     * collection, scoped to the given project.
     *
     * @param queryVector the dense vector for the search query (e.g. embedded claim)
     * @param projectId   the project to filter by (matches the {@code project_id} payload)
     * @return the MySQL chunk ID of the top-scoring point, or {@code null} if no
     *         results are found
     */
    public String findClosestChunkId(List<Float> queryVector, String projectId) {
        Map<String, Object> filter = Map.of(
                "must", List.of(
                        Map.of("key", "project_id",
                                "match", Map.of("value", projectId))
                )
        );

        Map<String, Object> body = Map.of(
                "vector", queryVector,
                "filter", filter,
                "limit", 1,
                "with_payload", false
        );

        String url = baseUrl + "/collections/" + COLLECTION + "/points/search";

        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new QdrantException("POST search", res.getStatusCode().value());
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");
            if (results == null || results.isEmpty()) {
                log.debug("Qdrant search returned no results for projectId={}", projectId);
                return null;
            }

            // The "id" field is a numeric point ID that maps 1:1 to MySQL chunk ID
            Object id = results.get(0).get("id");
            String chunkId = String.valueOf(id);
            log.debug("Qdrant top hit: chunkId={}, score={}", chunkId, results.get(0).get("score"));
            return chunkId;
        } catch (QdrantException e) {
            throw e;
        } catch (Exception e) {
            log.error("Qdrant search failed for projectId={}", projectId, e);
            throw new QdrantException("POST search", e.getMessage(), e);
        }
    }

    // ── Collection bootstrap ───────────────────────────────────────────────────

    /**
     * Creates the {@code source_chunks} collection if it does not exist.
     * Uses cosine distance to match the embedding model's expected similarity metric.
     */
    private void ensureCollection(int vectorSize) {
        if (collectionEnsured) {
            return;
        }

        synchronized (this) {
            if (collectionEnsured) {
                return;
            }

            String checkUrl = baseUrl + "/collections/" + COLLECTION;
            try {
                restClient.get()
                        .uri(checkUrl)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            if (res.getStatusCode().value() == 404) {
                                throw new CollectionNotFoundException();
                            }
                            throw new QdrantException("GET collection", res.getStatusCode().value());
                        })
                        .toBodilessEntity();

                log.info("Qdrant collection '{}' already exists", COLLECTION);
                collectionEnsured = true;
                return;
            } catch (CollectionNotFoundException ignored) {
                // Fall through to creation
            }

            // Create the collection with cosine distance
            Map<String, Object> createBody = Map.of(
                    "vectors", Map.of(
                            "size", vectorSize,
                            "distance", "Cosine"
                    )
            );

            try {
                restClient.put()
                        .uri(checkUrl)
                        .body(createBody)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            throw new QdrantException("PUT collection", res.getStatusCode().value());
                        })
                        .toBodilessEntity();

                log.info("Created Qdrant collection '{}' (vectorSize={}, distance=Cosine)",
                        COLLECTION, vectorSize);
                collectionEnsured = true;
            } catch (Exception e) {
                log.warn("Failed to create Qdrant collection '{}'. It may already exist " +
                         "from a concurrent request. Proceeding anyway.", COLLECTION, e);
                collectionEnsured = true;
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private static String trimTrailingSlash(String url) {
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** Sentinel exception used only inside ensureCollection to detect 404. */
    private static final class CollectionNotFoundException extends RuntimeException {
    }


}
