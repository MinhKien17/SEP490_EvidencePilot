package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.InstructorFeedback;
import java.time.LocalDateTime;

public record InstructorFeedbackResponseDto(
        Integer id,
        Integer requestId,
        Integer instructorId,
        String content,
        LocalDateTime createdAt
) {
    public static InstructorFeedbackResponseDto fromEntity(InstructorFeedback feedback) {
        if (feedback == null) return null;
        return new InstructorFeedbackResponseDto(
                feedback.getId(),
                feedback.getRequest() != null ? feedback.getRequest().getId() : null,
                feedback.getInstructor() != null ? feedback.getInstructor().getId() : null,
                feedback.getContent(),
                feedback.getCreatedAt()
        );
    }
}
