package com.evidencepilot.service;

import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceScoringServiceTest {

    @Test
    void scoresSupportiveExcerptWithoutReferencesAsMediumStrength() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setText("Evidence text");

        EvidenceScoringService.ScoreResult result = new EvidenceScoringService()
                .computeScore(EvidenceRelation.SUPPORTS, chunk, false, false, false, 0);

        assertThat(result.strengthScore()).isEqualTo(45);
        assertThat(result.strengthBand()).isEqualTo(StrengthBand.MEDIUM);
        assertThat(result.rubricVersion()).isEqualTo("1.1");
        assertThat(result.scoreBreakdown()).containsKeys(
                "relation", "evidence_anchor", "source_type_authority",
                "citation_metadata", "link_availability");
    }

    @Test
    void fullMetadataAndLocatorYieldHundredPoints() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkIndex(7);
        chunk.setText("Evidence text");

        EvidenceScoringService.ScoreResult result = new EvidenceScoringService()
                .computeScore(EvidenceRelation.SUPPORTS, chunk, true, true, true, 25);

        assertThat(result.strengthScore()).isEqualTo(100);
        assertThat(result.strengthBand()).isEqualTo(StrengthBand.HIGH);
        assertThat(result.scoreBreakdown())
                .containsEntry("source_type_authority", Map.of("max", 25, "earned", 25));
    }

    @Test
    void contradictionIsStrongRelationWhileNeutralIsNot() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setText("Evidence text");

        EvidenceScoringService service = new EvidenceScoringService();
        var contradiction = service.computeScore(
                EvidenceRelation.CONTRADICTS, chunk, false, false, false, 0);
        var neutral = service.computeScore(
                EvidenceRelation.NEUTRAL, chunk, false, false, false, 0);

        assertThat(contradiction.strengthScore()).isEqualTo(45);
        assertThat(neutral.strengthScore()).isEqualTo(10);
    }
}
