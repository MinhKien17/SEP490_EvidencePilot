package com.evidencepilot.dto.response;

import com.evidencepilot.model.InstructorFeedback;
import java.time.LocalDateTime;
import java.util.UUID;

public record InstructorFeedbackResponseDto(
    UUID id,
    UUID requestId,
    UUID sectionId,
    UUID instructorId,
    String lineReference,
    String content,
    LocalDateTime createdAt
) {
    public static InstructorFeedbackResponseDto fromEntity(InstructorFeedback feedback) {
        return new InstructorFeedbackResponseDto(
            feedback.getId(),
            feedback.getRequest() != null ? feedback.getRequest().getId() : null,
            feedback.getSection() != null ? feedback.getSection().getId() : null,
            feedback.getInstructor() != null ? feedback.getInstructor().getId() : null,
            feedback.getLineReference(),
            feedback.getContent(),
            feedback.getCreatedAt()
        );
    }
}
