package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.FeedbackRequest;
import com.evidencepilot.domain.enums.FeedbackStatus;
import java.time.LocalDateTime;

public record FeedbackRequestResponseDto(
        Integer id,
        Integer projectId,
        Integer studentId,
        Integer instructorId,
        FeedbackStatus status,
        LocalDateTime requestedAt
) {
    public static FeedbackRequestResponseDto fromEntity(FeedbackRequest request) {
        if (request == null) return null;
        return new FeedbackRequestResponseDto(
                request.getId(),
                request.getProject() != null ? request.getProject().getId() : null,
                request.getStudent() != null ? request.getStudent().getId() : null,
                request.getInstructor() != null ? request.getInstructor().getId() : null,
                request.getStatus(),
                request.getRequestedAt()
        );
    }
}
