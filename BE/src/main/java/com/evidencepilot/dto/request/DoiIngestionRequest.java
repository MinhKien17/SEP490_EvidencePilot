package com.evidencepilot.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record DoiIngestionRequest(
        @NotBlank String doi,
        UUID projectId,
        UUID collectionId
) {
    @AssertTrue(message = "Either projectId or collectionId must be provided")
    public boolean isValid() {
        return projectId != null || collectionId != null;
    }
}
