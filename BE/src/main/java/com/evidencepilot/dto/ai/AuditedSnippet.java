package com.evidencepilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditedSnippet(
        @JsonProperty("original_text_snippet") String originalTextSnippet,
        @JsonProperty("start_index") int startIndex,
        @JsonProperty("end_index") int endIndex,
        @JsonProperty("issue_type") String issueType,
        @JsonProperty("rationale") String rationale,
        @JsonProperty("suggested_paraphrase") String suggestedParaphrase) {
}
