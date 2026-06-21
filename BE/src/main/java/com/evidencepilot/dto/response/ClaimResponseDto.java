package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.Claim;
import java.math.BigDecimal;

public record ClaimResponseDto(
        Integer id,
        Integer projectId,
        String content,
        BigDecimal aiConfidenceScore,
        boolean active
) {
    public static ClaimResponseDto fromEntity(Claim claim) {
        if (claim == null) return null;
        return new ClaimResponseDto(
                claim.getId(),
                claim.getProject() != null ? claim.getProject().getId() : null,
                claim.getContent(),
                claim.getAiConfidenceScore(),
                claim.isActive()
        );
    }
}
