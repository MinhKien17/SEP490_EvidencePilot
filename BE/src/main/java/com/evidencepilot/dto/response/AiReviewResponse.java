package com.evidencepilot.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiReviewResponse(
        String reviewVersion,
        boolean complete,
        Coverage coverage,
        Direction direction,
        String summary,
        List<Finding> findings,
        List<String> limitations
) {
    public enum Direction {
        ON_TRACK,
        NEEDS_ATTENTION,
        INSUFFICIENT_DATA
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    public enum FindingType {
        MISSING_CLAIM,
        UNUSED_CLAIM,
        ORPHANED_CLAIM,
        UNSUPPORTED_CLAIM,
        REDUNDANT_CLAIM,
        UNNECESSARY_CLAIM,
        EXCESSIVE_CLAIMS,
        CLAIM_GAP,
        UNRESOLVED_FEEDBACK,
        OTHER
    }

    public AiReviewResponse {
        findings = findings == null ? List.of() : List.copyOf(findings);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    // ponytail: global rubric 0-5, derived so it can never drift from findings; FE reads it from JSON
    @JsonProperty("rubricScore")
    public Double rubricScore() {
        if (!isScorable()) {
            return null;
        }
        if (findings.isEmpty()) {
            return 5.0;
        }
        double sum = 0;
        for (Finding finding : findings) {
            sum += finding.score();
        }
        return Math.round(sum / findings.size() * 10) / 10.0;
    }

    @JsonProperty("passes")
    public boolean passes() {
        Double score = rubricScore();
        return score != null && score >= 3.5;
    }

    private boolean isScorable() {
        return complete
                && direction != Direction.INSUFFICIENT_DATA
                && coverage != null
                && coverage.totalSections() > 0
                && coverage.sectionsScanned() == coverage.totalSections()
                && coverage.totalChunks() > 0
                && coverage.chunksScanned() == coverage.totalChunks()
                && coverage.claimsChecked() == coverage.totalClaims();
    }

    public record Coverage(
            int totalSections,
            int sectionsScanned,
            int totalChunks,
            int chunksScanned,
            int totalClaims,
            int claimsChecked
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Finding(
            FindingType type,
            Severity severity,
            UUID claimId,
            UUID sectionId,
            List<UUID> sourceIds,
            List<UUID> feedbackIds,
            String excerpt,
            String message,
            String recommendedAction
    ) {
        public Finding {
            sourceIds = sourceIds == null ? List.of() : List.copyOf(sourceIds);
            feedbackIds = feedbackIds == null ? List.of() : List.copyOf(feedbackIds);
        }

        // ponytail: rubric 0-5 per finding, deterministic from type + severity cap
        @JsonProperty("score")
        public int score() {
            int severityCap = switch (severity) {
                case CRITICAL -> 1;
                case WARNING -> 3;
                case INFO -> 4;
            };
            int base = switch (type) {
                case ORPHANED_CLAIM -> 1;
                case UNUSED_CLAIM, UNSUPPORTED_CLAIM, MISSING_CLAIM, CLAIM_GAP, EXCESSIVE_CLAIMS -> 2;
                case REDUNDANT_CLAIM, UNNECESSARY_CLAIM, UNRESOLVED_FEEDBACK -> 3;
                case OTHER -> 4;
            };
            return Math.min(base, severityCap);
        }

        public boolean valid(
                Set<UUID> claimIds,
                Set<UUID> sectionIds,
                Set<UUID> sourceIdsAllowed,
                Set<UUID> feedbackIdsAllowed) {
            return type != null
                    && severity != null
                    && (claimId == null || claimIds.contains(claimId))
                    && (sectionId == null || sectionIds.contains(sectionId))
                    && sourceIds.stream().allMatch(sourceIdsAllowed::contains)
                    && feedbackIds.stream().allMatch(feedbackIdsAllowed::contains)
                    && excerpt != null
                    && message != null
                    && !message.isBlank()
                    && recommendedAction != null
                    && !recommendedAction.isBlank();
        }
    }
}
