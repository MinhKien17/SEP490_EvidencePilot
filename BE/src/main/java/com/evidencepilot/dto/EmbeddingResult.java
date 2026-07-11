package com.evidencepilot.dto;

import java.util.UUID;

public record EmbeddingResult(
        UUID jobId,
        UUID documentId,
        String status,
        String resultObjectKey,
        String error) {
}
