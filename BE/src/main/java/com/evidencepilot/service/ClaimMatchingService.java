package com.evidencepilot.service;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimMatchCandidateResponse;
import java.util.List;
import java.util.UUID;

public interface ClaimMatchingService {

    List<ClaimMatchCandidateResponse> searchMatches(UUID claimId, UUID projectId);

    AiSuggestionResponse evaluateMatch(UUID claimId, UUID projectId, UUID documentChunkId);
}
