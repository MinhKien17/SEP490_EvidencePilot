package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.FunctionalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimUpdateRequest(
    @NotBlank @Size(max = 5000) String content,
    FunctionalType functionalType
) {}
