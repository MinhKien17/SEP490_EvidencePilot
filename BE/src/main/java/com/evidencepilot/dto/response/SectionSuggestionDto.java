package com.evidencepilot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record SectionSuggestionDto(
        String type,
        String issue,
        String quote,
        @JsonProperty("actionable_fix") String actionableFix,
        Evidence evidence) {

    public record Evidence(
            @JsonProperty("chunk_id") UUID chunkId,
            @JsonProperty("source_id") UUID sourceId,
            String quote) {
    }
}