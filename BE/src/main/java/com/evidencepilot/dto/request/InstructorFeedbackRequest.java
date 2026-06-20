package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InstructorFeedbackRequest {
    @NotBlank
    @Size(max = 10000)
    private String content;
}
