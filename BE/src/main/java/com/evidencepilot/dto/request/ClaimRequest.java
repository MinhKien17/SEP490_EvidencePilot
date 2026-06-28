package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClaimRequest(
    @NotBlank String claimText
) {}
