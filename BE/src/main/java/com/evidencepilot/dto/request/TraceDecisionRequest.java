package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.StudentAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TraceDecisionRequest(
        @NotNull StudentAction studentAction,
        UUID sourceId,
        UUID chunkId,
        @Size(max = 1_200) String evidenceQuote,
        String relation,
        @Size(max = 2_000) String explanation,
        Boolean accepted,
        @Size(max = 64) String actualEditHash,
        Long roundDurationMs
) {
}