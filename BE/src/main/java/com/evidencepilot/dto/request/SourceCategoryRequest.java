package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourceCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        String description
) {}
