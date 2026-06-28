package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimUpdateRequest(
    @NotBlank @Size(max = 5000) String content
) {}
