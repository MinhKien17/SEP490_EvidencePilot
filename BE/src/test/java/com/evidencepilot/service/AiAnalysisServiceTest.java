package com.evidencepilot.service;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.ai.dto.ClaimAnalysisRequest;
import com.evidencepilot.ai.dto.ClaimAnalysisResponse;
import com.evidencepilot.ai.dto.ClaimMatch;
import com.evidencepilot.ai.dto.ClaimMatchResponse;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.EvidenceEdge;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.EvidenceEdgeRepository;
import com.evidencepilot.repository.SourceChunkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private EvidenceEdgeRepository evidenceEdgeRepository;

    @Mock
    private SourceChunkRepository sourceChunkRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ClaimMatchingService claimMatchingService;

    @InjectMocks
    private AiAnalysisService aiAnalysisService;

    @Test
    void analyzeAndPersistMatchesTopSourceThenPersistsConfidenceAndEvidenceEdge() {
        Claim claim = claim(42, "Smoking increases cardiovascular risk.");
        ClaimMatch match = new ClaimMatch(
                "1",
                "paper.pdf",
                "7",
                3,
                "Cardiovascular risk was higher among smokers.",
                new BigDecimal("0.91"),
                "strong",
                "Directly related"
        );
        ClaimAnalysisResponse response = new ClaimAnalysisResponse(
                "supported",
                new BigDecimal("0.8700"),
                List.of("1"),
                List.of(),
                "The cited excerpt supports the claim."
        );

        SourceChunk mockChunk = new SourceChunk();
        mockChunk.setId(7);
        mockChunk.setText("Cardiovascular risk was higher among smokers.");
        com.evidencepilot.domain.entity.Source mockSource = new com.evidencepilot.domain.entity.Source();
        mockSource.setId(1);
        mockChunk.setSource(mockSource);

        when(claimMatchingService.matchClaim(claim, 1))
                .thenReturn(new ClaimMatchResponse(claim.getContent(), List.of(match)));
        when(aiModelClient.processClaim(any(ClaimAnalysisRequest.class))).thenReturn(response);
        when(claimRepository.save(claim)).thenReturn(claim);
        when(sourceChunkRepository.findById(7)).thenReturn(Optional.of(mockChunk));

        Claim updated = aiAnalysisService.analyzeAndPersist(claim);

        assertThat(updated.getAiConfidenceScore()).isEqualByComparingTo("0.8700");

        ArgumentCaptor<ClaimAnalysisRequest> analysisRequest =
                ArgumentCaptor.forClass(ClaimAnalysisRequest.class);
        verify(aiModelClient).processClaim(analysisRequest.capture());
        assertThat(analysisRequest.getValue().sourceId()).isEqualTo("1");
        assertThat(analysisRequest.getValue().excerpt())
                .isEqualTo("Cardiovascular risk was higher among smokers.");

        ArgumentCaptor<EvidenceEdge> edgeCaptor = ArgumentCaptor.forClass(EvidenceEdge.class);
        verify(evidenceEdgeRepository).save(edgeCaptor.capture());
        EvidenceEdge edge = edgeCaptor.getValue();
        assertThat(edge.getClaim()).isSameAs(claim);
        assertThat(edge.getSourceChunk()).isSameAs(mockChunk);
        assertThat(edge.getVerdict()).isEqualTo("supported");
        assertThat(edge.getConfidenceScore()).isEqualByComparingTo("0.8700");
        assertThat(edge.getExplanation()).isEqualTo("The cited excerpt supports the claim.");
        assertThat(edge.getMissingEvidence()).isEqualTo("[]");
    }

    @Test
    void analyzeAndPersistRejectsWhenAiReturnsNoMatches() {
        Claim claim = claim(42, "Unsupported claim.");
        when(claimMatchingService.matchClaim(claim, 1))
                .thenReturn(new ClaimMatchResponse(claim.getContent(), List.of()));

        assertThatThrownBy(() -> aiAnalysisService.analyzeAndPersist(claim))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(aiModelClient, never()).processClaim(any());
        verify(claimRepository, never()).save(any());
        verify(evidenceEdgeRepository, never()).save(any());
    }

    @Test
    void analyzeAndPersistWrapsAiProcessErrorsAsServiceUnavailable() {
        Claim claim = claim(42, "Claim text.");
        when(aiModelClient.processClaim(any(ClaimAnalysisRequest.class)))
                .thenThrow(new AiModelClient.AiApiException("POST /process/claim", 500));

        assertThatThrownBy(() ->
                aiAnalysisService.analyzeAndPersist(claim, "1", "excerpt", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(claimRepository, never()).save(any());
        verify(evidenceEdgeRepository, never()).save(any());
    }

    private static Claim claim(Integer id, String content) {
        Claim claim = new Claim();
        claim.setId(id);
        claim.setContent(content);
        return claim;
    }
}
