package com.evidencepilot.service.impl;

import com.evidencepilot.dto.QdrantSearchResult;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.service.ClaimMatchingService;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.EvidenceScoringService;
import com.evidencepilot.service.QdrantClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimMatchingServiceImpl implements ClaimMatchingService {

    private static final int TOP_K = 20;

    private final ClaimRepository claimRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ClaimMapper claimMapper;
    private final AiModelClient aiModelClient;
    private final QdrantClient qdrantClient;
    private final EvidenceScoringService evidenceScoringService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<AiSuggestionResponse> matchClaim(UUID claimId, UUID projectId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(claimId, "Claim"));

        List<Float> embedding = aiModelClient.generateEmbedding(claim.getContent());
        List<QdrantSearchResult> matches = qdrantClient.findClosestChunks(
                embedding, "PROJECT", projectId.toString(), TOP_K);
        if (matches == null || matches.isEmpty()) {
            log.info("Created 0 suggestions for claim {}", claimId);
            return List.of();
        }

        List<AiSuggestion> suggestions = matches.stream()
                .map(match -> matchedSourceChunk(match, projectId)
                        .map(chunk -> buildSuggestion(claim, chunk, match)))
                .flatMap(Optional::stream)
                .toList();

        if (suggestions.isEmpty()) {
            log.info("Created 0 suggestions for claim {}", claimId);
            return List.of();
        }

        List<AiSuggestion> saved = aiSuggestionRepository.saveAll(suggestions);

        log.info("Created {} suggestions for claim {}", saved.size(), claimId);
        return saved.stream()
                .map(claimMapper::toAiSuggestionResponse)
                .toList();
    }

    private Optional<DocumentChunk> matchedSourceChunk(QdrantSearchResult match, UUID projectId) {
        UUID chunkId;
        try {
            chunkId = UUID.fromString(match.chunkId());
        } catch (IllegalArgumentException e) {
            log.warn("Qdrant returned invalid chunk id {}, skipping", match.chunkId());
            return Optional.empty();
        }

        return documentChunkRepository.findById(chunkId)
                .filter(DocumentChunk::isActive)
                .filter(chunk -> chunk.getDocument() != null)
                .filter(chunk -> chunk.getDocument().isActive())
                .filter(chunk -> chunk.getDocument().getDocType() == DocumentType.SOURCE)
                .filter(chunk -> isDocumentInProject(chunk.getDocument(), projectId));
    }

    private boolean isDocumentInProject(Document document, UUID projectId) {
        if (document.getProject() != null && projectId.equals(document.getProject().getId()))
            return true;
        return projectDocumentRepository.existsByProjectIdAndDocumentId(projectId, document.getId());
    }

    private AiSuggestion buildSuggestion(Claim claim, DocumentChunk chunk, QdrantSearchResult match) {
        EvidenceRelation relation = EvidenceRelation.SUPPORTS;
        EvidenceScoringService.ScoreResult strength = evidenceScoringService.computeScore(
                relation, chunk, List.of(), false);
        LocalDateTime evaluatedAt = LocalDateTime.now();

        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setClaim(claim);
        suggestion.setDocumentChunk(chunk);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setScore(match.score().floatValue());
        suggestion.setExplanation("Matched " + sourceName(chunk) + " chunk " + chunk.getChunkIndex());
        suggestion.setClaimVersion(claim.getClaimVersion());
        suggestion.setCreatedAt(evaluatedAt);
        suggestion.setModelName("ollama");
        suggestion.setModelVersion("nomic-embed-text");
        suggestion.setPromptVersion("v1");
        suggestion.setRubricVersion(strength.rubricVersion());
        suggestion.setEvaluatedAt(evaluatedAt);
        suggestion.setScoreBreakdown(serializeBreakdown(strength));
        suggestion.setRelation(relation);
        suggestion.setStrengthScore(strength.strengthScore());
        suggestion.setStrengthBand(strength.strengthBand());
        return suggestion;
    }

    private String serializeBreakdown(EvidenceScoringService.ScoreResult strength) {
        try {
            return objectMapper.writeValueAsString(strength.scoreBreakdown());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize evidence score breakdown", e);
        }
    }

    private String sourceName(DocumentChunk chunk) {
        String filename = chunk.getDocument().getOriginalFilename();
        return filename == null || filename.isBlank()
                ? chunk.getDocument().getId().toString()
                : filename;
    }
}
