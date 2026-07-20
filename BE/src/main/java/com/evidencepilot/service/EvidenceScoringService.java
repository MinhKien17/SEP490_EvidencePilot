package com.evidencepilot.service;

import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentReference;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvidenceScoringService {

    static final String RUBRIC_VERSION = "1.0";

    public ScoreResult computeScore(EvidenceRelation relation, DocumentChunk chunk,
                                    List<DocumentReference> references, boolean linkReachable) {
        int earned = 0;
        int applicable = 0;
        Map<String, Object> breakdown = new LinkedHashMap<>();

        int relationPoints = scoreRelation(relation);
        earned += relationPoints;
        applicable += 35;
        breakdown.put("relation", Map.of("max", 35, "earned", relationPoints));

        boolean hasExcerpt = chunk != null && chunk.getText() != null && !chunk.getText().isBlank();
        boolean hasLocator = false;
        if (hasExcerpt) {
            int anchorPoints = hasLocator ? 20 : 10;
            earned += anchorPoints;
            applicable += 20;
            breakdown.put("evidence_anchor", Map.of("max", 20, "earned", anchorPoints, "has_locator", hasLocator));
        } else {
            breakdown.put("evidence_anchor", Map.of("max", 20, "earned", 0));
        }

        applicable += 25;
        breakdown.put("source_type_authority", Map.of("max", 25, "earned", 0));

        int citationPoints = scoreCitationMetadata(references);
        earned += citationPoints;
        applicable += 10;
        breakdown.put("citation_metadata", Map.of("max", 10, "earned", citationPoints));

        int linkPoints = linkReachable ? 10 : 0;
        earned += linkPoints;
        applicable += 10;
        breakdown.put("link_availability", Map.of("max", 10, "earned", linkPoints, "reachable", linkReachable));

        int strengthScore = applicable > 0 ? (int) Math.round(100.0 * earned / applicable) : 0;
        StrengthBand band;
        if (strengthScore >= 70) band = StrengthBand.HIGH;
        else if (strengthScore >= 40) band = StrengthBand.MEDIUM;
        else band = StrengthBand.LOW;

        return new ScoreResult(strengthScore, band, RUBRIC_VERSION, breakdown);
    }

    private int scoreRelation(EvidenceRelation relation) {
        if (relation == null) return 0;
        return switch (relation) {
            case SUPPORTS, EXTENDS, DETAILS -> 35;
            case GENERALIZES -> 20;
            case NEUTRAL -> 10;
            case CONTRADICTS -> 0;
        };
    }

    private int scoreCitationMetadata(List<DocumentReference> references) {
        if (references == null || references.isEmpty()) return 0;
        DocumentReference ref = references.get(0);
        int points = 0;
        if (ref.getTitle() != null && !ref.getTitle().isBlank()) points += 2;
        if (ref.getRawText() != null && !ref.getRawText().isBlank()) points += 2;
        if (ref.getPublicationYear() != null) points += 2;
        if (ref.getDoi() != null && !ref.getDoi().isBlank()) points += 4;
        return Math.min(points, 10);
    }

    public record ScoreResult(
        int strengthScore,
        StrengthBand strengthBand,
        String rubricVersion,
        Map<String, Object> scoreBreakdown
    ) {}
}
