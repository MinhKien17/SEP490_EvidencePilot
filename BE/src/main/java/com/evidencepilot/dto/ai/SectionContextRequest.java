package com.evidencepilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Strict data payload sent to the Python AI server at {@code POST /audit/section}.
 * The Python server owns all prompt construction and grounds every snippet offset
 * against {@code sourceChunk}.
 */
public record SectionContextRequest(
        @JsonProperty("section_name") String sectionName,
        @JsonProperty("current_context") String currentContext,
        @JsonProperty("source_chunk") String sourceChunk,
        @JsonProperty("evaluation_criteria") List<EvaluationCriterion> evaluationCriteria,
        @JsonProperty("flags") SectionAuditFlags flags) {
}
