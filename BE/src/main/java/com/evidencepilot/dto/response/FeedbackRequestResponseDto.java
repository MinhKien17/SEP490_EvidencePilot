package com.evidencepilot.dto.response;

import com.evidencepilot.model.FeedbackRequest;
import com.evidencepilot.model.FeedbackStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackRequestResponseDto(
    UUID id,
    UUID projectId,
    UUID studentId,
    UUID instructorId,
    FeedbackStatus status,
    LocalDateTime requestedAt,
    String sectionValidation
) {
    public static FeedbackRequestResponseDto fromEntity(FeedbackRequest request) {
        return new FeedbackRequestResponseDto(
            request.getId(),
            request.getProject() != null ? request.getProject().getId() : null,
            request.getStudent() != null ? request.getStudent().getId() : null,
            request.getInstructor() != null ? request.getInstructor().getId() : null,
            request.getStatus(),
            request.getRequestedAt(),
            request.getSectionValidation()
        );
    }
}
