package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProgressReportResponse(
    UUID projectId,
    List<SectionPanel> sections,
    Readiness readiness
) {
    public record SectionPanel(
        UUID sectionId,
        String sectionTitle,
        int wordCount,
        UUID assignedUserId,
        String assignedUserName,
        int version,
        LocalDateTime lastUpdated,
        int feedbackAnswered,
        int feedbackUnanswered
    ) {}

    public record Readiness(
        int score,
        int contentCoveragePercent,
        List<ReadinessMetric> metrics
    ) {}

    public record ReadinessMetric(
        String code,
        String label,
        int weightPercent,
        int valuePercent
    ) {}
}
