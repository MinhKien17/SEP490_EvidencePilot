package com.evidencepilot.dto;

import java.util.UUID;

public record ExtractionRequest(UUID documentId, String s3ObjectKey, UUID userId) {}
