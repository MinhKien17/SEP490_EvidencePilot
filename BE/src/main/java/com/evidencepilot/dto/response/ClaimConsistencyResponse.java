package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.ClaimContentStatus;

import java.util.List;
import java.util.UUID;

public record ClaimConsistencyResponse(
        int warningCount,
        List<Warning> warnings
) {
    public ClaimConsistencyResponse {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public record Warning(
            UUID claimId,
            UUID sectionId,
            ClaimContentStatus status,
            String message
    ) {}
}
