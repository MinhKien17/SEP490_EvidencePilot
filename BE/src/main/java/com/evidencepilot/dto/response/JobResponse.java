package com.evidencepilot.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID projectId,
        String kind,
        String status,
        JsonNode result,
        String errorMessage,
        LocalDateTime completedAt) {}
