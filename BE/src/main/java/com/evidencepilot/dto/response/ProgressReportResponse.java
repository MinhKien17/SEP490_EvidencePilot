package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.ClaimContentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProgressReportResponse(
    UUID projectId,
    List<SectionPanel> sections,
    List<MatrixRow> matrix,
    Readiness readiness
) {
    public record SectionPanel(
        UUID sectionId,
        String sectionTitle,
        int wordCount,
        int claimCount,
        UUID assignedUserId,
        String assignedUserName,
        int version,
        LocalDateTime lastUpdated,
        int feedbackAnswered,
        int feedbackUnanswered
    ) {}

    public record MatrixRow(
        UUID claimId,
        String content,
        UUID sectionId,
        String sectionTitle,
        ClaimContentStatus contentStatus,
        int activeEvidenceCount,
        String strongestRelation,
        Integer strongestScore,
        UUID createdById,
        String createdByName,
        LocalDateTime updatedAt
    ) {}

    public record Readiness(
        int score,
        int contentCoveragePercent,
        int claimsPresentPercent,
        int claimsWithEvidencePercent
    ) {}
}
