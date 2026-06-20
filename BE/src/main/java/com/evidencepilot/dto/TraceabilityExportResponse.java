package com.evidencepilot.dto;

import com.evidencepilot.domain.enums.FeedbackStatus;
import com.evidencepilot.domain.enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TraceabilityExportResponse(
        Integer projectId,
        String projectTitle,
        ProjectStatus projectStatus,
        Instant generatedAt,
        List<TraceabilityClaim> claims,
        List<TraceabilitySource> sources,
        List<TraceabilityFeedback> feedback
) {
    public record TraceabilityClaim(
            Integer claimId,
            String content,
            BigDecimal confidence,
            Map<String, Object> graphData,
            List<TraceabilityMatch> matches
    ) {
    }

    public record TraceabilityMatch(
            String sourceId,
            String filename,
            String chunkId,
            Integer page,
            String excerpt,
            BigDecimal score,
            String suitability,
            String explanation,
            String citationTitle,
            String citationYear,
            String citationRawText
    ) {
    }

    public record TraceabilitySource(
            Integer sourceId,
            String filename,
            String contentType,
            Long fileSizeBytes,
            String fileUrl,
            int referenceCount
    ) {
    }

    public record TraceabilityFeedback(
            Integer requestId,
            Integer instructorId,
            FeedbackStatus status
    ) {
    }
}
