package com.evidencepilot.service;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.*;
import com.evidencepilot.repository.*;
import com.evidencepilot.service.impl.AiAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AiAnalysisServiceImplTest {

    private final ClaimMatchingService matching = mock(ClaimMatchingService.class);
    private final ClaimRepository claims = mock(ClaimRepository.class);
    private final DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
    private final ClaimMapper mapper = mock(ClaimMapper.class);
    private AiAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiAnalysisServiceImpl(matching, claims, chunks, mapper);
    }

    @Test
    void analyzeAndPersist_returnsClaimWhenNoSuggestionsExist() {
        Claim claim = claim();
        when(matching.matchClaim(claim.getId(), claim.getProject().getId())).thenReturn(List.of());

        assertThat(service.analyzeAndPersist(claim)).isSameAs(claim);
        verifyNoInteractions(claims, chunks);
    }

    @Test
    void analyzeAndPersist_averagesScoresAndCreatesEdges() {
        Claim claim = claim();
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = new DocumentChunk();
        when(matching.matchClaim(claim.getId(), claim.getProject().getId())).thenReturn(List.of(
                suggestion(chunkId, 0.8f, "strong"), suggestion(null, null, "unknown")));
        when(chunks.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(claims.save(claim)).thenReturn(claim);

        Claim saved = service.analyzeAndPersist(claim);

        assertThat(saved.getAiConfidenceScore()).isEqualTo(0.4f);
    }

    @Test
    void directAnalysis_rejectsInvalidOrMissingChunk() {
        Claim claim = claim();
        assertThat(service.analyzeAndPersist(claim, "bad", "excerpt", "title")).isSameAs(claim);

        UUID chunkId = UUID.randomUUID();
        assertThatThrownBy(() -> service.analyzeAndPersist(claim, chunkId.toString(), "excerpt", "title"))
                .hasMessageContaining("DocumentChunk");
    }

    @Test
    void directAnalysis_createsSupportiveEdgeAndConfidence() {
        Claim claim = claim();
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = new DocumentChunk();
        when(chunks.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(claims.save(claim)).thenReturn(claim);

        Claim saved = service.analyzeAndPersist(claim, chunkId.toString(), "excerpt", "title");

        assertThat(saved.getAiConfidenceScore()).isEqualTo(0.75f);
    }

    private static Claim claim() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        return claim;
    }

    private static AiSuggestionResponse suggestion(UUID chunkId, Float score, String explanation) {
        return new AiSuggestionResponse(
                UUID.randomUUID(), UUID.randomUUID(), chunkId, null, null, null, null,
                AiSuggestionResponse.PENDING, score, explanation, 1, null);
    }
}
