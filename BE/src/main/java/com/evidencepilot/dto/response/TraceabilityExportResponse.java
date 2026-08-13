package com.evidencepilot.dto.response;

import com.evidencepilot.model.FeedbackStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TraceabilityExportResponse(
    UUID projectId,
    String projectTitle,
    ProjectStatus projectStatus,
    Instant generatedAt,
    List<TraceabilitySection> sections,
    List<TraceabilitySource> sources,
    List<TraceabilityFeedback> feedback,
    List<TraceabilityTrace> traces
) {
    public TraceabilityExportResponse {
        traces = traces == null ? List.of() : List.copyOf(traces);
    }

    public record TraceabilitySection(
        UUID id,
        String title,
        Integer wordCount,
        Integer version,
        UUID assignedUserId
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

    public record TraceabilityTrace(
        UUID id,
        UUID sectionId,
        String sectionTitle,
        Integer findingIndex,
        String suggestedAction,
        String excerpt,
        UUID sourceId,
        String evidenceQuote,
        String evidenceRelation,
        String studentAction,
        String outcome,
        String judgment
    ) {}
}
