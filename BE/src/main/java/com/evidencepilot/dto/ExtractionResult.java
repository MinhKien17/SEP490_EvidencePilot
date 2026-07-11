package com.evidencepilot.dto;

import java.util.UUID;

public record ExtractionResult(
        UUID documentId,
        String status,
        String markdownObjectKey,
        String sha256,
        String extractionMethod,
        String error) {
}
