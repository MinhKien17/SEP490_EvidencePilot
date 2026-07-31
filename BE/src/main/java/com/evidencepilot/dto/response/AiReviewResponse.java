package com.evidencepilot.dto.response;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    public record Coverage(
            int totalSections,
            int sectionsScanned,
            int totalChunks,
            int chunksScanned,
            int totalClaims,
            int claimsChecked
    ) {}

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
