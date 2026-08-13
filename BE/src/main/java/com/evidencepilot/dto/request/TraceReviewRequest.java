package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.InstructorJudgment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TraceReviewRequest(
        @NotNull InstructorJudgment judgment,
        @Size(max = 2_000) String instructorFeedback
) {
}
