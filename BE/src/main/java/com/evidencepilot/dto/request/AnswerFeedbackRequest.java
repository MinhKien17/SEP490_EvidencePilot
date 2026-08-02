package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnswerFeedbackRequest(@NotBlank String content) {
}
