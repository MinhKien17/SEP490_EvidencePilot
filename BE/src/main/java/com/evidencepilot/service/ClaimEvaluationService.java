package com.evidencepilot.service;

import com.evidencepilot.dto.request.ClaimRequest;
import com.evidencepilot.dto.response.ClaimEvaluationResponse;

import java.util.UUID;

public interface ClaimEvaluationService {

    ClaimEvaluationResponse evaluate(UUID documentId, ClaimRequest request);
}
