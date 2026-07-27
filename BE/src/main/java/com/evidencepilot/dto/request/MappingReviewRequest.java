package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.MappingReviewStatus;
import jakarta.validation.constraints.NotNull;

public record MappingReviewRequest(
    @NotNull MappingReviewStatus reviewStatus,
    String reviewNote,
    String relationOverride
) {}
