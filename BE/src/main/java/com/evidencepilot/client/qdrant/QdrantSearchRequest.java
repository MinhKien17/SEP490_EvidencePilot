package com.evidencepilot.client.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record QdrantSearchRequest(
        List<Float> vector,
        Filter filter,
        int limit,
        @JsonProperty("with_payload") boolean withPayload,
        @JsonProperty("with_vector") boolean withVector
) {
    public record Filter(List<Condition> must) {}
    public record Condition(String key, Match match) {}
    public record Match(String value) {}

    public static QdrantSearchRequest forDocument(String documentId, List<Float> queryVector, int limit) {
        return new QdrantSearchRequest(
                queryVector,
                new Filter(List.of(new Condition("documentId", new Match(documentId)))),
                limit,
                true,
                false
        );
    }
}
