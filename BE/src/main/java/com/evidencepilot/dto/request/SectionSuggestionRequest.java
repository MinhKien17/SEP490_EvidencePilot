package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SectionSuggestionRequest(
        @NotBlank String sectionType) {
}
