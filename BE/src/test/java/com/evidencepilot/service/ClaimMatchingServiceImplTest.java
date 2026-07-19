package com.evidencepilot.service;

import com.evidencepilot.dto.QdrantSearchResult;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.service.impl.ClaimMatchingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimMatchingServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private AiSuggestionRepository aiSuggestionRepository;

    @Mock
    private ClaimMapper claimMapper;

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private QdrantClient qdrantClient;

    @Mock
    private ProjectDocumentRepository projectDocumentRepository;

    @Test
    void matchClaimUsesProjectScopedQdrantResultsAndReturnsChunkLocation() {
        UUID projectId = UUID.randomUUID();
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project(projectId));
        claim.setContent("Claim text");
        claim.setClaimVersion(1);

        Document source = document(DocumentType.SOURCE, projectId, "source-a.pdf");
        Document paper = document(DocumentType.PAPER, projectId, "paper.pdf");
        DocumentChunk sourceChunk = chunk(source, 3, "Evidence text");
        DocumentChunk paperChunk = chunk(paper, 0, "Paper text");
        List<Float> embedding = List.of(0.25f, -0.5f);

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(aiModelClient.generateEmbedding(claim.getContent())).thenReturn(embedding);
        when(qdrantClient.findClosestChunks(embedding, "PROJECT", projectId.toString(), 20))
                .thenReturn(List.of(
                        new QdrantSearchResult(sourceChunk.getId().toString(), new BigDecimal("0.82")),
                        new QdrantSearchResult(paperChunk.getId().toString(), new BigDecimal("0.91"))));
        when(documentChunkRepository.findById(sourceChunk.getId())).thenReturn(Optional.of(sourceChunk));
        when(documentChunkRepository.findById(paperChunk.getId())).thenReturn(Optional.of(paperChunk));
        when(aiSuggestionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(claimMapper.toAiSuggestionResponse(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
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
                    null, null, null, null, null, null, null, null, null);
        });

        ClaimMatchingServiceImpl service = new ClaimMatchingServiceImpl(
                claimRepository,
                documentChunkRepository,
                aiSuggestionRepository,
                projectDocumentRepository,
                claimMapper,
                aiModelClient,
                qdrantClient);

        List<AiSuggestionResponse> responses = service.matchClaim(claim.getId(), projectId);

        verify(qdrantClient).findClosestChunks(embedding, "PROJECT", projectId.toString(), 20);

        ArgumentCaptor<List<AiSuggestion>> suggestionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiSuggestionRepository).saveAll(suggestionsCaptor.capture());
        assertThat(suggestionsCaptor.getValue())
                .singleElement()
                .satisfies(suggestion -> {
                    assertThat(suggestion.getDocumentChunk()).isSameAs(sourceChunk);
                    assertThat(suggestion.getScore()).isEqualTo(0.82f);
                    assertThat(suggestion.getExplanation()).contains("source-a.pdf", "chunk 3");
                });
        assertThat(responses)
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.documentChunkId()).isEqualTo(sourceChunk.getId());
                    assertThat(response.documentId()).isEqualTo(source.getId());
                    assertThat(response.sourceFilename()).isEqualTo("source-a.pdf");
                    assertThat(response.chunkIndex()).isEqualTo(3);
                    assertThat(response.excerpt()).isEqualTo("Evidence text");
                });
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
}
