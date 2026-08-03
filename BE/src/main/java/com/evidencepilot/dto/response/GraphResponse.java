package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.ClaimContentStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GraphResponse(
    List<GraphClaim> claims,
    List<GraphSource> sources,
    List<GraphEdge> edges,
    List<GraphSectionSummary> sectionSummaries,
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
        ClaimContentStatus contentStatus,
        Map<String, Object> graphData,
        int matchCount
    ) {}

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

    public record ClaimStatsResponse(
        int totalClaims,
        Map<String, Double> byFunctionalType
    ) {}
}
