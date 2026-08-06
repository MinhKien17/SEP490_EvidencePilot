package com.evidencepilot.dto.response;

import com.evidencepilot.model.SectionAuditFinding;
import com.evidencepilot.model.enums.SectionAuditFindingStatus;
import com.evidencepilot.model.enums.SectionAuditIssueType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SectionAuditFindingResponse(
        UUID id,
        UUID sectionId,
        int startIndex,
        int endIndex,
        String originalTextSnippet,
        SectionAuditIssueType issueType,
        String suggestedParaphrase,
        String rationale,
        SectionAuditFindingStatus status,
        String modelName,
        LocalDateTime createdAt) {

    public static SectionAuditFindingResponse from(SectionAuditFinding finding) {
        return new SectionAuditFindingResponse(
                finding.getId(),
                finding.getSection().getId(),
                finding.getStartIndex(),
                finding.getEndIndex(),
                finding.getOriginalTextSnippet(),
                finding.getIssueType(),
                finding.getSuggestedParaphrase(),
                finding.getRationale(),
                finding.getStatus(),
                finding.getModelName(),
                finding.getCreatedAt());
    }
}
