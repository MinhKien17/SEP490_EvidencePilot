package com.evidencepilot.dto;

import com.evidencepilot.dto.response.AiReviewResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiReviewResponseTest {

    private AiReviewResponse response(AiReviewResponse.Finding... findings) {
        return new AiReviewResponse(
                "paper-claim-review-v3", true,
                new AiReviewResponse.Coverage(1, 1, 1, 1, 1, 1),
                AiReviewResponse.Direction.ON_TRACK,
                "summary", List.of(findings), List.of());
    }

    private AiReviewResponse.Finding finding(
            AiReviewResponse.FindingType type, AiReviewResponse.Severity severity) {
        return new AiReviewResponse.Finding(
                type, severity, UUID.randomUUID(), UUID.randomUUID(),
                List.of(), List.of(), "", "message", "action");
    }

    @Test
    void findingScoreFollowsRubricCappedBySeverity() {
        assertThat(finding(AiReviewResponse.FindingType.ORPHANED_CLAIM,
                AiReviewResponse.Severity.CRITICAL).score()).isEqualTo(1);
        assertThat(finding(AiReviewResponse.FindingType.UNSUPPORTED_CLAIM,
                AiReviewResponse.Severity.WARNING).score()).isEqualTo(2);
        assertThat(finding(AiReviewResponse.FindingType.REDUNDANT_CLAIM,
                AiReviewResponse.Severity.WARNING).score()).isEqualTo(3);
        assertThat(finding(AiReviewResponse.FindingType.OTHER,
                AiReviewResponse.Severity.INFO).score()).isEqualTo(4);
        assertThat(finding(AiReviewResponse.FindingType.MISSING_CLAIM,
                AiReviewResponse.Severity.CRITICAL).score()).isEqualTo(1);
        assertThat(finding(AiReviewResponse.FindingType.STRUCTURE_GAP,
                AiReviewResponse.Severity.INFO).score()).isEqualTo(2);
    }

    @Test
    void globalRubricIsMeanOfFindingScoresAndPassesAtThreshold() {
        AiReviewResponse clean = response();
        assertThat(clean.rubricScore()).isEqualTo(5.0);
        assertThat(clean.passes()).isTrue();

        AiReviewResponse mixed = response(
                finding(AiReviewResponse.FindingType.ORPHANED_CLAIM,
                        AiReviewResponse.Severity.CRITICAL),
                finding(AiReviewResponse.FindingType.REDUNDANT_CLAIM,
                        AiReviewResponse.Severity.WARNING),
                finding(AiReviewResponse.FindingType.OTHER,
                        AiReviewResponse.Severity.INFO));
        assertThat(mixed.rubricScore()).isEqualTo(2.7);
        assertThat(mixed.passes()).isFalse();

        AiReviewResponse borderline = response(
                finding(AiReviewResponse.FindingType.OTHER, AiReviewResponse.Severity.INFO),
                finding(AiReviewResponse.FindingType.OTHER, AiReviewResponse.Severity.INFO),
                finding(AiReviewResponse.FindingType.REDUNDANT_CLAIM,
                        AiReviewResponse.Severity.WARNING));
        assertThat(borderline.rubricScore()).isEqualTo(3.7);
        assertThat(borderline.passes()).isTrue();
    }

    @Test
    void insufficientReviewHasNoRubricScoreAndDoesNotPass() {
        AiReviewResponse response = new AiReviewResponse(
                "paper-claim-review-v4", false,
                new AiReviewResponse.Coverage(0, 0, 0, 0, 0, 0),
                AiReviewResponse.Direction.INSUFFICIENT_DATA,
                "No active Section content", List.of(), List.of());

        assertThat(response.rubricScore()).isNull();
        assertThat(response.passes()).isFalse();
    }
}
