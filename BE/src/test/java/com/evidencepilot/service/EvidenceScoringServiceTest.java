package com.evidencepilot.service;

import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceScoringServiceTest {

    @Test
    void pillarScoresSumIntoStrengthScoreAndBand() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setText("Evidence text");

        EvidenceScoringService.ScoreResult result = new EvidenceScoringService()
                .computeScore(EvidenceRelation.SUPPORTS, chunk, 0.75f, 35, 15);

        assertThat(result.strengthScore()).isEqualTo(80);
        assertThat(result.strengthBand()).isEqualTo(StrengthBand.HIGH);
        assertThat(result.rubricVersion()).isEqualTo("2.0");
        assertThat(result.scoreBreakdown()).containsKeys(
                "semantic_alignment", "contextual_sufficiency", "logical_restraint");
    }

    @Test
    void cosineIsClampedAndScaledToForty() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setText("Evidence text");

        EvidenceScoringService service = new EvidenceScoringService();
        var negative = service.computeScore(EvidenceRelation.NEUTRAL, chunk, -0.5f, 0, 0);
        var overOne = service.computeScore(EvidenceRelation.NEUTRAL, chunk, 1.5f, 0, 0);

        assertThat(negative.strengthScore()).isZero();
        assertThat(overOne.strengthScore()).isEqualTo(40);
        assertThat(overOne.strengthBand()).isEqualTo(StrengthBand.MEDIUM);
    }

    @Test
    void aiPillarScoresAreClampedToTheirMaxima() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setText("Evidence text");

        EvidenceScoringService.ScoreResult result = new EvidenceScoringService()
                .computeScore(EvidenceRelation.SUPPORTS, chunk, 0f, 99, -10);

        assertThat(result.strengthScore()).isEqualTo(40);
        assertThat(result.scoreBreakdown())
                .containsEntry("contextual_sufficiency", Map.of("max", 40, "earned", 40));
    }

    @Test
    void cosineOfVectors() {
        assertThat(EvidenceScoringService.cosine(List.of(1f, 0f), List.of(1f, 0f))).isEqualTo(1f);
        assertThat(EvidenceScoringService.cosine(List.of(1f, 0f), List.of(0f, 1f))).isZero();
        assertThat(EvidenceScoringService.cosine(List.of(1f, 0f), List.of(1f))).isZero();
        assertThat(EvidenceScoringService.cosine(List.of(), List.of())).isZero();
        assertThat(EvidenceScoringService.cosine(null, List.of(1f))).isZero();
    }
}
