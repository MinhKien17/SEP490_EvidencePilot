package com.evidencepilot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SectionReviewSourceMatchRequest(
        @NotEmpty @Size(max = 10) List<@Valid Finding> findings
) {
    public record Finding(
            @PositiveOrZero int findingIndex,
            @NotBlank @Size(max = 1_000) String excerpt,
            @PositiveOrZero int startOffset,
            @PositiveOrZero int endOffset
    ) {
    }
}
