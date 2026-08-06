package com.evidencepilot.dto.response;

import java.util.List;
import java.util.UUID;

public record SectionReviewSourceMatchesResponse(
        List<FindingMatches> findings
) {
    public SectionReviewSourceMatchesResponse {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public record FindingMatches(
            int findingIndex,
            List<SourceCandidate> candidates
    ) {
        public FindingMatches {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }

    public record SourceCandidate(
            UUID documentChunkId,
            UUID documentId,
            String citationKey,
            String sourceFilename,
            String title,
            String authors,
            Integer publicationYear,
            String doi,
            String excerpt,
            float similarityScore
    ) {
    }
}
