package com.evidencepilot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SectionSuggestionDto(
        String issue,
        String quote,
        @JsonProperty("actionable_fix") String actionableFix) {
}
