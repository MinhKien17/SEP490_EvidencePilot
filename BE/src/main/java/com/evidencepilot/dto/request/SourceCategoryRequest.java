package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SourceCategoryRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*")
        String code,
        @NotBlank @Size(max = 100) String name,
        String description
) {}
