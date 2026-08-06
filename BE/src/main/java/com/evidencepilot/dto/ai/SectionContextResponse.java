package com.evidencepilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SectionContextResponse(
        @JsonProperty("snippets") List<AuditedSnippet> snippets) {
}
