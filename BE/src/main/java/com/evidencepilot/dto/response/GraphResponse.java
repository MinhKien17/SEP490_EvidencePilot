package com.evidencepilot.dto.response;

import java.util.List;
import java.util.UUID;

public record GraphResponse(
    List<GraphSource> sources,
    List<GraphEdge> edges,
    List<GraphSectionSummary> sectionSummaries,
    int totalEdges,
    boolean hasMore
) {
    public record GraphSource(
        UUID id,
        String filename,
        int referenceCount,
        String topic
    ) {}

    public record GraphEdge(
        String sourceId,
        String targetId,
        String relation,
        Integer score
    ) {}

    public record GraphSectionSummary(
        UUID sectionId,
        String sectionTitle,
        int claimCount,
        int presentCount,
        int missingCount,
        int orphanedCount,
        int unsupportedCount,
        UUID assignedUserId,
        String assignedUserName
    ) {}
}
