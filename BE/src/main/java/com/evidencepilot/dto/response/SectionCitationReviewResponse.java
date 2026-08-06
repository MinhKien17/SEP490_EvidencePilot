package com.evidencepilot.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SectionCitationReviewResponse(
        String reviewVersion,
        String ruleCatalogVersion,
        UUID sectionId,
        Integer sectionVersion,
        String contentFingerprint,
        LocalDateTime reviewedAt,
        String provider,
        String model,
        boolean complete,
        String summary,
        List<Finding> findings,
        List<String> limitations
) {
    public SectionCitationReviewResponse {
        findings = findings == null ? List.of() : List.copyOf(findings);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public enum RuleCode {
        EXTERNAL_FACT_OR_DEFINITION,
        QUANTITATIVE_OR_STATISTICAL_CLAIM,
        PRIOR_WORK_OR_COMPARISON,
        ATTRIBUTED_METHOD_DATASET_OR_STANDARD,
        CAUSAL_OR_GENERALIZABLE_CLAIM
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Finding(
            RuleCode ruleCode,
            String excerpt,
            int startOffset,
            int endOffset,
            String reason,
            String recommendedAction
    ) {
    }
}
