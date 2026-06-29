package com.evidencepilot.dto.response;

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
    LocalDateTime createdAt
) {
    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String INVALIDATED = "INVALIDATED";
}
