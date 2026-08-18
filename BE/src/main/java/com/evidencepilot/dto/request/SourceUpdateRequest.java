package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourceUpdateRequest(
        @NotBlank @Size(max = 255) String title
) {}
