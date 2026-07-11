package com.evidencepilot.dto;

import java.util.UUID;

public record EmbeddingRequest(
        String kind,
        UUID jobId,
        UUID documentId,
        String manifestObjectKey) {
}
