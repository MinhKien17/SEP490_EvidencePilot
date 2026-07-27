package com.evidencepilot.service;

import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.MappingReviewStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GapDetectionService {

    public GapResult analyzeGaps(List<ClaimEvidenceMapping> mappings) {
        boolean hasVerified = false;
        boolean allWeakOrIrrelevant = true;
        boolean contradicted = false;
        boolean hasPending = false;

        for (ClaimEvidenceMapping m : mappings) {
            if (m.getReviewStatus() == MappingReviewStatus.VERIFIED) {
                hasVerified = true;
                boolean strongEnough = m.getStrengthScore() != null && m.getStrengthScore() >= 40;
                boolean relevant = m.getRelation() != EvidenceRelation.NEUTRAL
                        && m.getRelation() != EvidenceRelation.CONTRADICTS;
                if (strongEnough && relevant) {
                    allWeakOrIrrelevant = false;
                }
            }
            if (m.getReviewStatus() == MappingReviewStatus.PENDING) {
                hasPending = true;
            }
            if (m.getRelation() == EvidenceRelation.CONTRADICTS) {
                contradicted = true;
            }
        }

        boolean unsupported = !hasVerified;
        boolean weak = hasVerified && allWeakOrIrrelevant;

        return new GapResult(unsupported, weak, contradicted, hasPending);
    }

    public record GapResult(
        boolean unsupported,
        boolean weak,
        boolean contradicted,
        boolean pendingSuggestions
    ) {}
}
