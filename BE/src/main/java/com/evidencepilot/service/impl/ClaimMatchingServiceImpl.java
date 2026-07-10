package com.evidencepilot.service.impl;

import com.evidencepilot.dto.QdrantSearchResult;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.service.ClaimMatchingService;
import com.evidencepilot.service.OllamaGateway;
import com.evidencepilot.service.QdrantClient;
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
    private final ClaimMapper claimMapper;
    private final OllamaGateway ollamaGateway;
    private final QdrantClient qdrantClient;

    @Override
    @Transactional
    public List<AiSuggestionResponse> matchClaim(UUID claimId, UUID projectId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(claimId, "Claim"));

        List<Float> embedding = ollamaGateway.getDenseEmbedding(claim.getContent());
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
                .filter(chunk -> chunk.getDocument().getProject() != null)
                .filter(chunk -> projectId.equals(chunk.getDocument().getProject().getId()));
    }

    private AiSuggestion buildSuggestion(Claim claim, DocumentChunk chunk, QdrantSearchResult match) {
        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setClaim(claim);
        suggestion.setDocumentChunk(chunk);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setScore(match.score().floatValue());
        suggestion.setExplanation("Matched " + sourceName(chunk) + " chunk " + chunk.getChunkIndex());
        suggestion.setClaimVersion(claim.getClaimVersion());
        suggestion.setCreatedAt(LocalDateTime.now());
        return suggestion;
    }

    private String sourceName(DocumentChunk chunk) {
        String filename = chunk.getDocument().getOriginalFilename();
        return filename == null || filename.isBlank()
                ? chunk.getDocument().getId().toString()
                : filename;
    }
}
