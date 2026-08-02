package com.evidencepilot.service;

import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Academic claim-source rubric (v2.0).
 *
 * <p>strengthScore (0-100) = semanticAlignment (0-40) + contextualSufficiency (0-40)
 * + logicalRestraint (0-20).</p>
 *
 * <ul>
 *   <li><b>Semantic Alignment (0-40):</b> cosine similarity between the claim and the source
 *       chunk embeddings, clamped to [0,1] and scaled by 40. Computed by the backend, not the AI.</li>
 *   <li><b>Contextual Sufficiency (0-40):</b> AI-scored — does the chunk carry concrete evidence
 *       (data, quotes, proven facts, mechanisms) for the claim, or only shared keywords?</li>
 *   <li><b>Logical Restraint (0-20):</b> AI-scored — overstatement penalty. A claim that oversteps
 *       what the source proves (source "sometimes" -> claim "always") earns 0 here.</li>
 * </ul>
 *
 * <p>Bands: &gt;=70 HIGH, &gt;=40 MEDIUM, else LOW.</p>
 */
@Service
public class EvidenceScoringService {

    static final String RUBRIC_VERSION = "2.0";

    /**
     * Score one claim-source evaluation.
     *
     * @param relation               AI relation judgment (kept for the evidence map, not scored)
     * @param chunk                  the source chunk (may be null for locator-free paths)
     * @param cosineScore            cosine(embedding(claim), embedding(chunk)); untrusted range [-1,1]
     * @param contextualSufficiency  AI score 0-40; clamped
     * @param logicalRestraint       AI score 0-20; clamped
     */
    public ScoreResult computeScore(EvidenceRelation relation, DocumentChunk chunk,
                                    float cosineScore, int contextualSufficiency, int logicalRestraint) {
        int semanticAlignment = Math.round(Math.max(0f, Math.min(1f, cosineScore)) * 40f);
        int sufficiency = clamp(contextualSufficiency, 0, 40);
        int restraint = clamp(logicalRestraint, 0, 20);
        int strengthScore = semanticAlignment + sufficiency + restraint;

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("semantic_alignment", Map.of("max", 40, "earned", semanticAlignment, "cosine", cosineScore));
        breakdown.put("contextual_sufficiency", Map.of("max", 40, "earned", sufficiency));
        breakdown.put("logical_restraint", Map.of("max", 20, "earned", restraint));

        StrengthBand band;
        if (strengthScore >= 70) band = StrengthBand.HIGH;
        else if (strengthScore >= 40) band = StrengthBand.MEDIUM;
        else band = StrengthBand.LOW;

        return new ScoreResult(strengthScore, band, RUBRIC_VERSION, breakdown);
    }

    /**
     * Cosine similarity of two dense vectors. Zero/empty/unequal-length vectors yield 0.
     */
    public static float cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return 0f;
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            float x = a.get(i);
            float y = b.get(i);
            dot += (double) x * y;
            normA += (double) x * x;
            normB += (double) y * y;
        }
        if (normA == 0 || normB == 0) return 0f;
        return (float) (dot / Math.sqrt(normA * normB));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record ScoreResult(
        int strengthScore,
        StrengthBand strengthBand,
        String rubricVersion,
        Map<String, Object> scoreBreakdown
    ) {}
}
