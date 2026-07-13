package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record DoiIngestionRequest(
        @NotBlank String doi,
        UUID projectId
) {}
