package com.evidencepilot.service;

import com.evidencepilot.dto.response.JobResponse;
import com.evidencepilot.dto.response.JobSubmitResponse;

import java.util.UUID;

public interface AiEvaluationService {
    JobSubmitResponse submit(UUID projectId, String kind, String payloadJson);

    JobSubmitResponse submitSectionCitationReview(
            UUID projectId,
            UUID documentId,
            UUID sectionId,
            String contentFingerprint,
            UUID requestedByUserId);

    JobSubmitResponse submitSectionSuggestion(
            UUID projectId,
            UUID documentId,
            UUID sectionId,
            String sectionType);

    void process(UUID jobId);

    JobResponse getJob(UUID jobId);
}
