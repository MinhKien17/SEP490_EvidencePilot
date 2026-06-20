package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewRequest {
    @NotNull
    private Integer instructorId;
}
