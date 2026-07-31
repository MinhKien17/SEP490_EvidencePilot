package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.model.enums.FunctionalType;

public record ClaimResponse(
    UUID id,
    UUID projectId,
    UUID sectionId,
    String content,
    Float aiConfidenceScore,
    FunctionalType functionalType,
    ClaimContentStatus contentStatus,
    Integer claimVersion,
    boolean active,
    UUID createdById,
    LocalDateTime createdAt
) {}
