package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DoiLookupRequest(
        @NotBlank String doi
) {}
