package com.evidencepilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractionResultMessage(
    UUID documentId,
    String status,
    Integer totalChunks
) {}
