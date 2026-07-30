package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClaimResponse(
    UUID id,
    UUID projectId,
    UUID sectionId,
    String content,
    Float aiConfidenceScore,
    Integer claimVersion,
    boolean active,
    UUID createdById,
    LocalDateTime createdAt
) {}
