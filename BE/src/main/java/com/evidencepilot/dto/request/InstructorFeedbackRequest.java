package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InstructorFeedbackRequest(
    @NotNull UUID sectionId,
    @Size(max = 100) String lineReference,
    @NotBlank String content
) {}
