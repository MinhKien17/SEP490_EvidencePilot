package com.evidencepilot.dto.response;

import com.evidencepilot.model.FeedbackStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TraceabilityExportResponse(
    UUID projectId,
    String projectTitle,
    ProjectStatus projectStatus,
    Instant generatedAt,
    List<TraceabilityClaim> claims,
    List<TraceabilitySource> sources,
    List<TraceabilityFeedback> feedback
) {
    public record TraceabilityClaim(
        UUID id,
        String content,
        Float aiConfidenceScore,
        Map<String, Object> graphData,
        List<TraceabilityMatch> matches,
        boolean unsupported,
        boolean weak,
        boolean contradicted,
        boolean pendingSuggestions
    ) {}

    public record TraceabilityMatch(
        String sourceId,
        String filename,
        UUID chunkId,
        Integer page,
        String excerpt,
        Float score,
        String suitability,
        String explanation,
        String referenceTitle,
        String referenceYear,
        String referenceText
    ) {}

    public record TraceabilitySource(
        UUID id,
        String filename,
        String contentType,
        Long fileSizeBytes,
        String fileUrl,
        int referenceCount
    ) {}

    public record TraceabilityFeedback(
        UUID id,
        UUID instructorId,
        FeedbackStatus status
    ) {}
}
