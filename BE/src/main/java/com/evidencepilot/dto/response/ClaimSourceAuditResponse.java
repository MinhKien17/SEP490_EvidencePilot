package com.evidencepilot.dto.response;

import java.util.List;
import java.util.UUID;

public record ClaimSourceAuditResponse(
    UUID projectId,
    int totalClaims,
    int totalMappings,
    int claimsWithNoSources,
    int claimsWithWeakSources,
    List<ClaimAuditItem> claims
) {
    public record ClaimAuditItem(
        UUID claimId,
        String content,
        UUID sectionId,
        int mappingCount,
        List<MappingAuditItem> mappings
    ) {}

    public record MappingAuditItem(
        UUID mappingId,
        String sourceFilename,
        boolean sourceActive,
        boolean chunkActive,
        Integer strengthScore,
        String strengthBand,
        String reviewStatus
    ) {}
}
