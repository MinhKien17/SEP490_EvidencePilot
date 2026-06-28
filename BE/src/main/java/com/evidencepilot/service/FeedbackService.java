package com.evidencepilot.service;

import com.evidencepilot.dto.request.InstructorFeedbackRequest;
import com.evidencepilot.dto.request.SubmitReviewRequest;
import com.evidencepilot.dto.response.FeedbackRequestResponseDto;
import com.evidencepilot.dto.response.InstructorFeedbackResponseDto;
import java.util.List;
import java.util.UUID;

public interface FeedbackService {
    List<FeedbackRequestResponseDto> findAllForCurrentUser();
    FeedbackRequestResponseDto submitForReview(UUID projectId, SubmitReviewRequest request);
    InstructorFeedbackResponseDto comment(UUID feedbackRequestId, InstructorFeedbackRequest request);
    FeedbackRequestResponseDto updateStatus(UUID feedbackRequestId, String status);
}
