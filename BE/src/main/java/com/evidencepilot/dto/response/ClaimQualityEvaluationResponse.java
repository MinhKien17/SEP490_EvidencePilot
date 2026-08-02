package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.FunctionalType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record ClaimQualityEvaluationResponse(
        String rubricVersion,
        List<Criterion> criteria,
        int totalScore,
        Decision decision,
        FunctionalType suggestedFunctionalType,
        String suggestedRevision
) {
    public static final String RUBRIC_VERSION = "claim-quality-v1";

    public enum CriterionCode {
        CLARITY,
        SPECIFICITY_SCOPE,
        SECTION_RELEVANCE,
        VERIFIABILITY_ARGUABILITY,
        ATOMICITY
    }

    public enum Decision {
        READY,
        REVISE
    }

    public record Criterion(CriterionCode code, int score, String reason) {
        public Criterion {
            if (code == null) {
                throw new IllegalArgumentException("Criterion code is required");
            }
            if (score < 0 || score > 2) {
                throw new IllegalArgumentException("Criterion score must be between 0 and 2");
            }
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Criterion reason is required");
            }
            reason = reason.strip();
        }
    }

    public static ClaimQualityEvaluationResponse from(
            List<Criterion> criteria,
            FunctionalType suggestedFunctionalType,
            String suggestedRevision) {
        if (criteria == null) {
            throw new IllegalArgumentException("Criteria are required");
        }
        if (suggestedFunctionalType == null) {
            throw new IllegalArgumentException("Suggested functional type is required");
        }
        if (suggestedRevision == null || suggestedRevision.isBlank()) {
            throw new IllegalArgumentException("Suggested revision is required");
        }

        Map<CriterionCode, Criterion> byCode = new EnumMap<>(CriterionCode.class);
        for (Criterion criterion : criteria) {
            if (criterion == null || byCode.putIfAbsent(criterion.code(), criterion) != null) {
                throw new IllegalArgumentException("Each criterion must appear exactly once");
            }
        }
        if (byCode.size() != CriterionCode.values().length) {
            throw new IllegalArgumentException("All five Claim Quality criteria are required");
        }

        List<Criterion> ordered = new ArrayList<>(CriterionCode.values().length);
        int total = 0;
        for (CriterionCode code : CriterionCode.values()) {
            Criterion criterion = byCode.get(code);
            ordered.add(criterion);
            total += criterion.score();
        }
        boolean coreCriteriaPresent = byCode.get(CriterionCode.CLARITY).score() > 0
                && byCode.get(CriterionCode.SECTION_RELEVANCE).score() > 0
                && byCode.get(CriterionCode.VERIFIABILITY_ARGUABILITY).score() > 0;
        Decision decision = total >= 8 && coreCriteriaPresent
                ? Decision.READY : Decision.REVISE;

        return new ClaimQualityEvaluationResponse(
                RUBRIC_VERSION,
                List.copyOf(ordered),
                total,
                decision,
                suggestedFunctionalType,
                suggestedRevision.strip());
    }
}
