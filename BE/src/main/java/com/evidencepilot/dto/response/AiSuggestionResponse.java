package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import java.time.LocalDateTime;
import java.util.UUID;

public record AiSuggestionResponse(
    UUID id,
    UUID claimId,
    UUID documentChunkId,
    UUID documentId,
    String sourceFilename,
    Integer chunkIndex,
    String excerpt,
    String status,
    Float score,
    String explanation,
    Integer claimVersion,
    LocalDateTime createdAt,
    String modelName,
    String modelVersion,
    String promptVersion,
    String rubricVersion,
    LocalDateTime evaluatedAt,
    String scoreBreakdown,
    EvidenceRelation relation,
    Integer strengthScore,
    StrengthBand strengthBand
) {
    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String INVALIDATED = "INVALIDATED";
}
