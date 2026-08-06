package com.evidencepilot.service;

import com.evidencepilot.dto.QdrantSearchResult;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimMatchCandidateResponse;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.service.impl.ClaimMatchingServiceImpl;
import com.evidencepilot.service.impl.SourceMatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimMatchingServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private AiSuggestionRepository aiSuggestionRepository;
    @Mock
    private ProjectDocumentRepository projectDocumentRepository;
    @Mock
    private ClaimMapper claimMapper;
    @Mock
    private AiModelClient aiModelClient;
    @Mock
    private QdrantClient qdrantClient;

    @Test
    void searchMatchesReturnsTransientSourceCandidatesOnly() {
        UUID projectId = UUID.randomUUID();
        Claim claim = claim(projectId);
        Document source = document(DocumentType.SOURCE, projectId, "source.pdf");
        Document paper = document(DocumentType.PAPER, projectId, "paper.pdf");
        DocumentChunk sourceChunk = chunk(source, 3, "Selected evidence");
        DocumentChunk paperChunk = chunk(paper, 0, "Draft text");
        List<Float> embedding = List.of(0.25f, -0.5f);

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                projectId, DocumentType.SOURCE)).thenReturn(List.of(source));
        when(aiModelClient.generateEmbeddings(List.of(claim.getContent())))
                .thenReturn(List.of(embedding));
        when(qdrantClient.findClosestChunks(
                embedding, List.of(source.getId().toString()), 20))
                .thenReturn(List.of(
                        new QdrantSearchResult(sourceChunk.getId().toString(), new BigDecimal("0.82")),
                        new QdrantSearchResult(paperChunk.getId().toString(), new BigDecimal("0.91"))));
        when(documentChunkRepository.findByIdWithDocument(sourceChunk.getId())).thenReturn(Optional.of(sourceChunk));
        when(documentChunkRepository.findByIdWithDocument(paperChunk.getId())).thenReturn(Optional.of(paperChunk));

        List<ClaimMatchCandidateResponse> candidates = service()
                .searchMatches(claim.getId(), projectId);

        assertThat(candidates)
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.documentChunkId()).isEqualTo(sourceChunk.getId());
                    assertThat(candidate.documentId()).isEqualTo(source.getId());
                    assertThat(candidate.sourceFilename()).isEqualTo("source.pdf");
                    assertThat(candidate.chunkIndex()).isEqualTo(3);
                    assertThat(candidate.excerpt()).isEqualTo("Selected evidence");
                    assertThat(candidate.similarityScore()).isEqualTo(0.82f);
                });
        verify(aiModelClient, never()).generate(anyString(), anyString());
        verifyNoInteractions(aiSuggestionRepository);
    }

    @Test
    void evaluateMatchSendsOnlySelectedChunkAndPersistsPendingEvaluation() {
        UUID projectId = UUID.randomUUID();
        Claim claim = claim(projectId);
        Document source = document(DocumentType.SOURCE, projectId, "source.pdf");
        DocumentChunk chunk = chunk(source, 2, "Exact selected chunk");

        when(claimRepository.findByIdWithProject(claim.getId())).thenReturn(Optional.of(claim));
        when(documentChunkRepository.findByIdWithDocument(chunk.getId())).thenReturn(Optional.of(chunk));
        when(aiSuggestionRepository
                .findFirstByClaimIdAndClaimVersionAndDocumentChunkIdOrderByCreatedAtDesc(
                        claim.getId(), claim.getClaimVersion(), chunk.getId()))
                .thenReturn(Optional.empty());
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(generated(
                "gemini",
                "gemini-3.6-flash",
                "{\"relation\":\"SUPPORTS\",\"explanation\":\"The chunk directly supports the claim.\","
                        + "\"contextualSufficiency\":30,\"contextualSufficiencyReason\":\"Concrete data present.\","
                        + "\"logicalRestraint\":15,\"logicalRestraintReason\":\"Stays within source.\"}"));
        when(aiModelClient.generateEmbedding(claim.getContent())).thenReturn(List.of(1f, 0f));
        when(aiModelClient.generateEmbedding("Exact selected chunk")).thenReturn(List.of(1f, 0f));
        when(aiSuggestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(claimMapper.toAiSuggestionResponse(any())).thenAnswer(invocation ->
                response(invocation.getArgument(0)));

        AiSuggestionResponse response = service()
                .evaluateMatch(claim.getId(), projectId, chunk.getId());

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).generate(system.capture(), prompt.capture());
        assertThat(system.getValue())
                .contains("academic evidence evaluator", "Return raw JSON only")
                .doesNotContain(claim.getContent(), "Exact selected chunk");
        assertThat(prompt.getValue())
                .contains(claim.getContent(), "Exact selected chunk")
                .doesNotContain("Draft text");
        verify(aiModelClient, times(2)).generateEmbedding(any());
        verifyNoInteractions(qdrantClient);

        ArgumentCaptor<AiSuggestion> saved = ArgumentCaptor.forClass(AiSuggestion.class);
        verify(aiSuggestionRepository).save(saved.capture());
        assertThat(saved.getValue().getScore()).isNull();
        assertThat(saved.getValue().getRelation()).isEqualTo(EvidenceRelation.SUPPORTS);
        assertThat(saved.getValue().getStrengthScore()).isEqualTo(85);
        assertThat(saved.getValue().getStrengthBand()).isEqualTo(StrengthBand.HIGH);
        assertThat(saved.getValue().getModelName()).isEqualTo("gemini");
        assertThat(saved.getValue().getModelVersion()).isEqualTo("gemini-3.6-flash");
        assertThat(saved.getValue().getPromptVersion()).isEqualTo("claim-evidence-v2");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.documentChunkId()).isEqualTo(chunk.getId());
    }

    @Test
    void evaluateMatchRejectsMalformedAiResponseWithoutSaving() {
        UUID projectId = UUID.randomUUID();
        Claim claim = claim(projectId);
        DocumentChunk chunk = chunk(
                document(DocumentType.SOURCE, projectId, "source.pdf"),
                1,
                "Evidence");

        when(claimRepository.findByIdWithProject(claim.getId())).thenReturn(Optional.of(claim));
        when(documentChunkRepository.findByIdWithDocument(chunk.getId())).thenReturn(Optional.of(chunk));
        when(aiSuggestionRepository
                .findFirstByClaimIdAndClaimVersionAndDocumentChunkIdOrderByCreatedAtDesc(
                        claim.getId(), claim.getClaimVersion(), chunk.getId()))
                .thenReturn(Optional.empty());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenReturn(generated("ollama", "qwen3.5:9b", "Supported"));

        assertThatThrownBy(() -> service().evaluateMatch(claim.getId(), projectId, chunk.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid claim evaluation");
        verify(aiSuggestionRepository, never()).save(any());
    }

    @Test
    void orthogonalEmbeddingYieldsZeroSemanticAlignmentPillar() throws Exception {
        UUID projectId = UUID.randomUUID();
        Claim claim = claim(projectId);
        DocumentChunk chunk = chunk(
                document(DocumentType.SOURCE, projectId, "source.pdf"),
                2,
                "Exact selected chunk");

        when(claimRepository.findByIdWithProject(claim.getId())).thenReturn(Optional.of(claim));
        when(documentChunkRepository.findByIdWithDocument(chunk.getId())).thenReturn(Optional.of(chunk));
        when(aiSuggestionRepository
                .findFirstByClaimIdAndClaimVersionAndDocumentChunkIdOrderByCreatedAtDesc(
                        claim.getId(), claim.getClaimVersion(), chunk.getId()))
                .thenReturn(Optional.empty());
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(generated(
                "ollama",
                "qwen3.5:9b",
                "{\"relation\":\"SUPPORTS\",\"explanation\":\"Direct support.\","
                        + "\"contextualSufficiency\":40,\"contextualSufficiencyReason\":\"Full evidence.\","
                        + "\"logicalRestraint\":20,\"logicalRestraintReason\":\"No overreach.\"}"));
        when(aiModelClient.generateEmbedding(claim.getContent())).thenReturn(List.of(1f, 0f));
        when(aiModelClient.generateEmbedding("Exact selected chunk")).thenReturn(List.of(0f, 1f));
        when(aiSuggestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(claimMapper.toAiSuggestionResponse(any())).thenAnswer(invocation ->
                response(invocation.getArgument(0)));

        AiSuggestionResponse response = service()
                .evaluateMatch(claim.getId(), projectId, chunk.getId());

        assertThat(response.strengthScore()).isEqualTo(60);
        assertThat(response.strengthBand()).isEqualTo(StrengthBand.MEDIUM);
        var breakdown = new ObjectMapper().readTree(response.scoreBreakdown());
        assertThat(breakdown.path("semantic_alignment").path("earned").asInt()).isZero();
        assertThat(breakdown.path("contextual_sufficiency").path("earned").asInt()).isEqualTo(40);
        assertThat(breakdown.path("logical_restraint").path("earned").asInt()).isEqualTo(20);
    }

    private ClaimMatchingServiceImpl service() {
        SourceMatchingService sourceMatchingService = new SourceMatchingService(
                documentRepository,
                projectDocumentRepository,
                documentChunkRepository,
                aiModelClient,
                qdrantClient);
        return new ClaimMatchingServiceImpl(
                claimRepository,
                documentChunkRepository,
                aiSuggestionRepository,
                projectDocumentRepository,
                claimMapper,
                aiModelClient,
                sourceMatchingService,
                new EvidenceScoringService(),
                new ObjectMapper());
    }

    private Claim claim(UUID projectId) {
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project(projectId));
        claim.setContent("Claim text");
        claim.setClaimVersion(1);
        claim.setActive(true);
        return claim;
    }

    private Project project(UUID id) {
        Project project = new Project();
        project.setId(id);
        return project;
    }

    private Document document(DocumentType type, UUID projectId, String filename) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project(projectId));
        document.setDocType(type);
        document.setOriginalFilename(filename);
        document.setActive(true);
        return document;
    }

    private DocumentChunk chunk(Document document, int chunkIndex, String text) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(document);
        chunk.setChunkIndex(chunkIndex);
        chunk.setText(text);
        chunk.setActive(true);
        return chunk;
    }

    private AiSuggestionResponse response(AiSuggestion suggestion) {
        DocumentChunk chunk = suggestion.getDocumentChunk();
        return new AiSuggestionResponse(
                suggestion.getId(),
                suggestion.getClaim().getId(),
                chunk.getId(),
                chunk.getDocument().getId(),
                chunk.getDocument().getOriginalFilename(),
                chunk.getChunkIndex(),
                chunk.getText(),
                suggestion.getStatus().name(),
                suggestion.getScore(),
                suggestion.getExplanation(),
                suggestion.getClaimVersion(),
                suggestion.getCreatedAt(),
                suggestion.getModelName(),
                suggestion.getModelVersion(),
                suggestion.getPromptVersion(),
                suggestion.getRubricVersion(),
                suggestion.getEvaluatedAt(),
                suggestion.getScoreBreakdown(),
                suggestion.getRelation(),
                suggestion.getStrengthScore(),
                suggestion.getStrengthBand());
    }

    private AiModelClient.GenerationResult generated(
            String provider, String model, String response) {
        return new AiModelClient.GenerationResult(provider, model, response);
    }
}
