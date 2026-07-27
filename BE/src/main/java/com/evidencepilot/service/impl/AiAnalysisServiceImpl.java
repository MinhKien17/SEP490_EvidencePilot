package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.service.AiAnalysisService;
import com.evidencepilot.service.ClaimMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final ClaimMatchingService claimMatchingService;
    private final ClaimRepository claimRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ClaimMapper claimMapper;

    @Override
    @Transactional
    public Claim analyzeAndPersist(Claim claim) {
        List<AiSuggestionResponse> suggestions = claimMatchingService.matchClaim(
                claim.getId(), claim.getProject().getId());

        if (suggestions.isEmpty()) {
            log.warn("No matching suggestions found for claim: {}", claim.getId());
            return claim;
        }

        double avgScore = suggestions.stream()
                .mapToDouble(s -> s.score() != null ? s.score() : 0.0)
                .average()
                .orElse(0.0);

        claim.setAiConfidenceScore((float) avgScore);
        Claim saved = claimRepository.save(claim);

        // EvidenceEdge creation removed in Phase 5 - using ClaimEvidenceMapping + AiSuggestion instead
        // for (AiSuggestionResponse suggestion : suggestions) {
        //     EvidenceEdge edge = new EvidenceEdge();
        //     edge.setClaim(saved);
        //     if (suggestion.documentChunkId() != null) {
        //         DocumentChunk chunk = documentChunkRepository.findById(suggestion.documentChunkId()).orElse(null);
        //         edge.setDocumentChunk(chunk);
        //     }
        //     edge.setVerdict("SUPPORTIVE");
        //     edge.setConfidenceScore(suggestion.score());
        //     edge.setExplanation(suggestion.explanation());
        //     edge.setCreatedAt(LocalDateTime.now());
        //     evidenceEdgeRepository.save(edge);
        // }

        log.info("Analysis completed for claim {} (score={})", claim.getId(), avgScore);
        return saved;
    }

    @Override
    @Transactional
    public Claim analyzeAndPersist(Claim claim, String sourceId, String excerpt, String title) {
        UUID chunkId;
        try {
            chunkId = UUID.fromString(sourceId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sourceId UUID {}, skipping evidence edge creation", sourceId);
            return claim;
        }

        DocumentChunk chunk = documentChunkRepository.findById(chunkId).orElse(null);
        if (chunk == null) {
            throw new ResourceNotFoundException("DocumentChunk not found for sourceId: " + sourceId);
        }

        // EvidenceEdge creation removed in Phase 5 - using ClaimEvidenceMapping + AiSuggestion instead
        // EvidenceEdge edge = new EvidenceEdge();
        // edge.setClaim(claim);
        // edge.setDocumentChunk(chunk);
        // edge.setVerdict("SUPPORTIVE");
        // edge.setConfidenceScore(0.75f);
        // edge.setExplanation(excerpt);
        // edge.setCreatedAt(LocalDateTime.now());
        // evidenceEdgeRepository.save(edge);

        claim.setAiConfidenceScore(0.75f);
        Claim saved = claimRepository.save(claim);

        log.info("Direct analysis completed for claim {} against chunk {}", claim.getId(), chunkId);
        return saved;
    }
}
