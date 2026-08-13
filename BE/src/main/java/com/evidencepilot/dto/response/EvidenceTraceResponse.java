package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.InstructorJudgment;
import com.evidencepilot.model.enums.StudentAction;
import com.evidencepilot.model.enums.TraceOutcome;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EvidenceTraceResponse(
        UUID id,
        UUID roundId,
        UUID sectionId,
        String sectionTitle,
        Integer sectionVersion,
        Integer findingIndex,
        String suggestedAction,
        String criticality,
        String parentHeader,
        String excerpt,
        Integer excerptStart,
        Integer excerptEnd,
        String rationale,
        BigDecimal confidence,
        UUID sourceId,
        String sourceTitle,
        UUID chunkId,
        String evidenceQuote,
        String evidenceRelation,
        StudentAction studentAction,
        String explanation,
        String afterPassage,
        Integer afterSectionVersion,
        TraceOutcome outcome,
        UUID instructorId,
        InstructorJudgment judgment,
        String instructorFeedback,
        LocalDateTime judgedAt,
        UUID linkedRoundId,
        String linkedMode,
        LocalDateTime createdAt
) {
}
