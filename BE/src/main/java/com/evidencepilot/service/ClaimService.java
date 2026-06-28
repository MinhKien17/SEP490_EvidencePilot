package com.evidencepilot.service;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.EvidenceEdgeResponse;
import com.evidencepilot.dto.response.PagedResponse;
import java.util.List;
import java.util.UUID;

public interface ClaimService {
    List<ClaimResponse> getAllClaims();
    PagedResponse<ClaimResponse> getAllClaims(
            int page,
            int size,
            String sort,
            String q,
            Boolean active);
    ClaimResponse getClaimById(UUID id);
    List<ClaimResponse> getClaimsByProject(UUID projectId);
    PagedResponse<ClaimResponse> getClaimsByProject(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            Boolean active);
    ClaimResponse createClaim(ClaimCreationRequest request);
    ClaimResponse updateClaim(UUID id, String content, Float aiConfidenceScore);
    void deleteClaim(UUID id);
    List<AiSuggestionResponse> getSuggestionsForClaim(UUID claimId);
    AiSuggestionResponse createSuggestion(UUID claimId, UUID documentChunkId, Float score, String explanation);
    void acceptSuggestion(UUID suggestionId);
    void rejectSuggestion(UUID suggestionId);
    void updateSuggestionStatus(UUID suggestionId, String status);
    List<ClaimEvidenceMappingResponse> getMappingsForClaim(UUID claimId);
    List<EvidenceEdgeResponse> getEdgesForClaim(UUID claimId);
}
