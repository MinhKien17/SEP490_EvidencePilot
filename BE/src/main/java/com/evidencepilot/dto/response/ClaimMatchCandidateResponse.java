package com.evidencepilot.dto.response;

import java.util.UUID;

public record ClaimMatchCandidateResponse(
        UUID documentChunkId,
        UUID documentId,
        String sourceFilename,
        Integer chunkIndex,
        String excerpt,
        Float similarityScore
) {
}
