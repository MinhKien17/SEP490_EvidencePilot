package com.evidencepilot.service.impl;

import com.evidencepilot.dto.NamedVectors;
import com.evidencepilot.dto.QdrantSearchResult;
import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.dto.UpsertBody;
import com.evidencepilot.dto.UpsertPoint;
import com.evidencepilot.exception.QdrantException;
import com.evidencepilot.service.QdrantClient;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class QdrantClientImpl implements QdrantClient {

    private static final String COLLECTION = "source_chunks";

    private final RestClient restClient;
    private final String baseUrl;

    private volatile boolean collectionEnsured = false;

    public QdrantClientImpl(
            @Value("${qdrant.url}") String qdrantUrl,
            @Value("${qdrant.api-key}") String qdrantApiKey) {
        this.baseUrl = trimTrailingSlash(qdrantUrl);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        var builder = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (qdrantApiKey != null && !qdrantApiKey.isBlank()) {
            builder.defaultHeader("api-key", qdrantApiKey);
        }

        this.restClient = builder.build();

        log.info("QdrantClient initialized – base URL: {}", this.baseUrl);
    }

    // ── Write ──────────────────────────────────────────────────────────────────

    @Override
    public void upsertVectors(List<QdrantClient.VectorPoint> points) {
        if (points.isEmpty()) {
            return;
        }
        ensureCollection(points.getFirst().denseVector().size());

        List<UpsertPoint> upsertPoints = new ArrayList<>(points.size());
        for (QdrantClient.VectorPoint vectorPoint : points) {
            String normalizedScopeType = normalizeScopeType(vectorPoint.scopeType());
            String normalizedScopeId = normalizeScopeId(vectorPoint.scopeId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("scope_type", normalizedScopeType);
            payload.put("scope_id", normalizedScopeId);
            if ("PROJECT".equals(normalizedScopeType)) {
                payload.put("project_id", normalizedScopeId);
            }
            if ("COLLECTION".equals(normalizedScopeType)) {
                payload.put("collection_id", normalizedScopeId);
            }
            payload.putAll(vectorPoint.extraPayload());
            upsertPoints.add(new UpsertPoint(
                    vectorPoint.chunkId(),
                    new NamedVectors(vectorPoint.denseVector(), vectorPoint.sparseVector()),
                    payload));
        }

        UpsertBody body = new UpsertBody(upsertPoints);
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
            log.debug("Upserted {} points into Qdrant", points.size());
        } catch (RestClientException e) {
            throw new QdrantException("Failed to sync vector to Qdrant", e);
        }
    }

    @Override
    public void deleteVectors(List<String> chunkIds) {
        if (chunkIds.isEmpty()) {
            return;
        }
        try {
            restClient.post()
                    .uri(baseUrl + "/collections/" + COLLECTION + "/points/delete?wait=true")
                    .body(Map.of("points", chunkIds))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        throw new QdrantException("Failed to delete stale Qdrant points");
                    })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new QdrantException("Failed to delete stale Qdrant points", e);
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    @Override
    public String findClosestChunkId(List<Float> queryVector, String projectId) {
        return findClosestChunks(queryVector, "PROJECT", projectId, 1).stream()
                .findFirst()
                .map(QdrantSearchResult::chunkId)
                .orElse(null);
    }

    @Override
    public List<QdrantSearchResult> findClosestChunks(
            List<Float> queryVector,
            String scopeType,
            String scopeId,
            int topK) {
        int safeTopK = Math.max(1, Math.min(topK, 20));
        String normalizedScopeType = normalizeScopeType(scopeType);
        String normalizedScopeId = normalizeScopeId(scopeId);
        Map<String, Object> filter = Map.of(
                "must", List.of(
                        Map.of("key", "scope_type",
                                "match", Map.of("value", normalizedScopeType)),
                        Map.of("key", "scope_id",
                                "match", Map.of("value", normalizedScopeId))
                )
        );

        Map<String, Object> body = Map.of(
                "query", queryVector,
                "using", "dense",
                "filter", filter,
                "limit", safeTopK,
                "with_payload", false
        );

        String url = baseUrl + "/collections/" + COLLECTION + "/points/query";

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
                return List.of();
            }

            List<Map<String, Object>> results = resultPoints(response.get("result"));
            if (results == null || results.isEmpty()) {
                log.debug("Qdrant search returned no results for scopeType={}, scopeId={}",
                        normalizedScopeType, normalizedScopeId);
                return List.of();
            }

            List<QdrantSearchResult> matches = new ArrayList<>();
            for (Map<String, Object> result : results) {
                Object id = result.get("id");
                if (id == null) {
                    continue;
                }
                matches.add(new QdrantSearchResult(String.valueOf(id), score(result.get("score"))));
            }
            log.debug("Qdrant returned {} hits for scopeType={}, scopeId={}",
                    matches.size(), normalizedScopeType, normalizedScopeId);
            return matches;
        } catch (QdrantException e) {
            throw e;
        } catch (Exception e) {
            log.error("Qdrant search failed for scopeType={}, scopeId={}",
                    normalizedScopeType, normalizedScopeId, e);
            throw new QdrantException("POST search", e.getMessage(), e);
        }
    }

    // ── Collection bootstrap ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> resultPoints(Object result) {
        if (result instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        if (result instanceof Map<?, ?> map) {
            Object points = map.get("points");
            if (points instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
        }
        return List.of();
    }

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
                ensurePayloadIndexes();
                collectionEnsured = true;
                return;
            } catch (CollectionNotFoundException ignored) {
                // Fall through to creation
            }

            // Create the collection with named vectors (dense + sparse)
            Map<String, Object> createBody = Map.of(
                    "vectors", Map.of(
                            "dense", Map.of("size", vectorSize, "distance", "Cosine")
                    ),
                    "sparse_vectors", Map.of(
                            "sparse", Map.of("modifier", "idf")
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

                log.info("Created Qdrant collection '{}' with named vectors (dense/sparse)",
                        COLLECTION);
                ensurePayloadIndexes();
                collectionEnsured = true;
            } catch (Exception e) {
                log.warn("Failed to create Qdrant collection '{}'. It may already exist " +
                         "from a concurrent request. Proceeding anyway.", COLLECTION, e);
                collectionEnsured = true;
            }
        }
    }

    private void ensurePayloadIndexes() {
        Map<String, String> schemas = Map.of(
                "scope_type", "keyword",
                "scope_id", "keyword",
                "project_id", "uuid",
                "document_id", "uuid");
        for (Map.Entry<String, String> entry : schemas.entrySet()) {
            try {
                restClient.put()
                        .uri(baseUrl + "/collections/" + COLLECTION + "/index?wait=true")
                        .body(Map.of("field_name", entry.getKey(), "field_schema", entry.getValue()))
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientException e) {
                log.debug("Payload index {} already exists or could not be created", entry.getKey());
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

    private static String normalizeScopeType(String scopeType) {
        if (scopeType == null || scopeType.isBlank()) {
            return "PROJECT";
        }
        return scopeType.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeScopeId(String scopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return "0";
        }
        return scopeId.trim();
    }

    private static BigDecimal score(Object rawScore) {
        if (rawScore instanceof BigDecimal decimal) {
            return decimal;
        }
        if (rawScore instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (rawScore == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(rawScore));
    }

    private static final class CollectionNotFoundException extends RuntimeException {
    }

}
