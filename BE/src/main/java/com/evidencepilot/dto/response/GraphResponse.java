package com.evidencepilot.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GraphResponse(
    List<GraphClaim> claims,
    List<GraphSource> sources,
    List<GraphEdge> edges,
    int totalEdges,
    boolean hasMore
) {
    public record GraphClaim(
        UUID id,
        String content,
        UUID sectionId,
        String sectionTitle,
        UUID createdById,
        String createdByName,
        Map<String, Object> graphData,
        int matchCount
    ) {}

    public record GraphSource(
        UUID id,
        String filename,
        int referenceCount
    ) {}

    public record GraphEdge(
        String sourceId,
        String targetId,
        String relation,
        Integer score
    ) {}
}
