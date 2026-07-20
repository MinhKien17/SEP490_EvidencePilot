package com.evidencepilot.service;

import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceScoringServiceTest {

    @Test
    void scoresSupportiveExcerptWithoutReferencesAsMediumStrength() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setText("Evidence text");

        EvidenceScoringService.ScoreResult result = new EvidenceScoringService()
                .computeScore(EvidenceRelation.SUPPORTS, chunk, List.of(), false);

        assertThat(result.strengthScore()).isEqualTo(45);
        assertThat(result.strengthBand()).isEqualTo(StrengthBand.MEDIUM);
        assertThat(result.rubricVersion()).isEqualTo("1.0");
        assertThat(result.scoreBreakdown()).containsKeys(
                "relation", "evidence_anchor", "source_type_authority",
                "citation_metadata", "link_availability");
    }
}
