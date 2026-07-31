package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import com.evidencepilot.model.enums.ClaimContentStatus;

public record ClaimResponse(
    UUID id,
    UUID projectId,
    UUID sectionId,
    String content,
    Float aiConfidenceScore,
    ClaimContentStatus contentStatus,
    Integer claimVersion,
    boolean active,
    UUID createdById,
    LocalDateTime createdAt
) {}
