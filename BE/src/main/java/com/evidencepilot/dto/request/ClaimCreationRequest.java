package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ClaimCreationRequest(
    @NotNull UUID sectionId,
    @NotBlank @Size(max = 5000) String content,
    Float aiConfidenceScore
) {}
