package com.evidencepilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record QdrantSearchRequest(
        List<Prefetch> prefetch,
        Map<String, String> query,
        Filter filter,
        int limit,
        @JsonProperty("with_payload") boolean withPayload
) {
    public record Prefetch(Object query, @JsonProperty("using") String using, int limit) {}
    public record Filter(List<Condition> must) {}
    public record Condition(String key, Match match) {}
    public record Match(String value) {}

    public static QdrantSearchRequest forDocument(String documentId, List<Float> denseVector, SparseVector sparseVector, int limit) {
        return new QdrantSearchRequest(
                List.of(
                        new Prefetch(denseVector, "dense", limit),
                        new Prefetch(sparseVector, "sparse", limit)
                ),
                Map.of("fusion", "rrf"),
                new Filter(List.of(new Condition("document_id", new Match(documentId)))),
                limit,
                true
        );
    }
}
