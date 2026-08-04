package com.evidencepilot.service;

import com.evidencepilot.dto.response.JobResponse;
import com.evidencepilot.dto.response.JobSubmitResponse;

import java.util.UUID;

public interface AiEvaluationService {
    JobSubmitResponse submit(UUID projectId, String kind, String payloadJson);

    JobSubmitResponse submitPaperReview(
            UUID projectId, UUID documentId, String targetStyle, UUID requestedByUserId);

    void process(UUID jobId);

    JobResponse getJob(UUID jobId);
}
