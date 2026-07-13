package com.evidencepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.impl.DocumentExtractionWorkerImpl;
import com.evidencepilot.service.impl.DocumentPersistenceService;
import com.evidencepilot.service.impl.SparseVectorGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionWorkerTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentObjectStorage documentObjectStorage;
    @Mock
    private AiModelClient aiModelClient;
    @Mock
    private SparseVectorGenerator sparseVectorGenerator;
    @Mock
    private QdrantService qdrantService;
    @Mock
    private DocumentPersistenceService persistence;

    @Test
    void processExtractsChunksEmbedsAndMarksReadyAfterQdrant() {
        UUID documentId = UUID.randomUUID();
        Document document = document(documentId);
        String markdown = "First paragraph.\n\nSecond paragraph.";
        String checkpointKey = "documents/processed/" + documentId + "/extraction.json";
        List<Float> vector = Collections.nCopies(768, 0.1f);
        DocumentChunk chunk = chunk(document, markdown);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentObjectStorage.exists(checkpointKey)).thenReturn(false);
        when(aiModelClient.extractDocument(eq("source.pdf"), anyString()))
                .thenReturn(extracted(markdown));
        when(aiModelClient.generateEmbeddings(List.of(markdown))).thenReturn(List.of(vector));
        when(sparseVectorGenerator.generate(markdown))
                .thenReturn(new SparseVector(List.of(1L), List.of(0.5f)));
        when(persistence.saveExtraction(documentId, "mineru", markdown, List.of(markdown)))
                .thenReturn(List.of(chunk));

        worker().process(documentId);

        verify(persistence).markProcessing(documentId);
        verify(documentObjectStorage).write(eq(checkpointKey), any(byte[].class), eq("application/json"));
        verify(documentObjectStorage, never()).exists("documents/processed/" + documentId + "/document.md");
        ArgumentCaptor<ExtractionResultPayload> payload = ArgumentCaptor.forClass(ExtractionResultPayload.class);
        InOrder completion = inOrder(qdrantService, persistence);
        completion.verify(qdrantService).upsertVectors(payload.capture());
        completion.verify(persistence).markReady(documentId, 1);
        assertThat(payload.getValue().chunks().getFirst().denseEmbedding()).hasSize(768);
    }

    @Test
    void processReusesExtractionCheckpointOnRetry() throws Exception {
        UUID documentId = UUID.randomUUID();
        Document document = document(documentId);
        String markdown = "cached markdown";
        String checkpointKey = "documents/processed/" + documentId + "/extraction.json";
        DocumentChunk chunk = chunk(document, markdown);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentObjectStorage.exists(checkpointKey)).thenReturn(true);
        when(documentObjectStorage.readText(checkpointKey))
                .thenReturn(new ObjectMapper().writeValueAsString(extracted(markdown)));
        when(aiModelClient.generateEmbeddings(any())).thenReturn(List.of(Collections.nCopies(768, 0.1f)));
        when(sparseVectorGenerator.generate(markdown))
                .thenReturn(new SparseVector(List.of(), List.of()));
        when(persistence.saveExtraction(documentId, "mineru", markdown, List.of(markdown)))
                .thenReturn(List.of(chunk));

        worker().process(documentId);

        verify(aiModelClient, never()).extractDocument(any(), any());
        verify(persistence).markReady(documentId, 1);
    }

    @Test
    void processReextractsWhenCheckpointIsInvalid() {
        UUID documentId = UUID.randomUUID();
        Document document = document(documentId);
        String markdown = "fresh markdown";
        String checkpointKey = "documents/processed/" + documentId + "/extraction.json";
        DocumentChunk chunk = chunk(document, markdown);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentObjectStorage.exists(checkpointKey)).thenReturn(true);
        when(documentObjectStorage.readText(checkpointKey)).thenReturn(
                "{\"filename\":\"source.pdf\",\"method\":\"mineru\",\"markdown\":\"legacy\"}");
        when(aiModelClient.extractDocument(eq("source.pdf"), anyString()))
                .thenReturn(extracted(markdown));
        when(aiModelClient.generateEmbeddings(List.of(markdown)))
                .thenReturn(List.of(Collections.nCopies(768, 0.1f)));
        when(sparseVectorGenerator.generate(markdown))
                .thenReturn(new SparseVector(List.of(), List.of()));
        when(persistence.saveExtraction(documentId, "mineru", markdown, List.of(markdown)))
                .thenReturn(List.of(chunk));

        worker().process(documentId);

        verify(aiModelClient).extractDocument(eq("source.pdf"), anyString());
        verify(documentObjectStorage).write(eq(checkpointKey), any(byte[].class), eq("application/json"));
    }

    @Test
    void processReextractsWhenCheckpointContainsNullBlock() {
        UUID documentId = UUID.randomUUID();
        Document document = document(documentId);
        String markdown = "fresh markdown";
        String checkpointKey = "documents/processed/" + documentId + "/extraction.json";
        DocumentChunk chunk = chunk(document, markdown);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentObjectStorage.exists(checkpointKey)).thenReturn(true);
        when(documentObjectStorage.readText(checkpointKey)).thenReturn(
                "{\"markdown\":\"broken\",\"blocks\":[null]}");
        when(aiModelClient.extractDocument(eq("source.pdf"), anyString()))
                .thenReturn(extracted(markdown));
        when(aiModelClient.generateEmbeddings(List.of(markdown)))
                .thenReturn(List.of(Collections.nCopies(768, 0.1f)));
        when(sparseVectorGenerator.generate(markdown))
                .thenReturn(new SparseVector(List.of(), List.of()));
        when(persistence.saveExtraction(documentId, "mineru", markdown, List.of(markdown)))
                .thenReturn(List.of(chunk));

        worker().process(documentId);

        verify(aiModelClient).extractDocument(eq("source.pdf"), anyString());
        verify(documentObjectStorage).write(eq(checkpointKey), any(byte[].class), eq("application/json"));
    }

    @Test
    void processMarksFailedWhenExtractionFails() {
        UUID documentId = UUID.randomUUID();
        Document document = document(documentId);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentObjectStorage.exists(any())).thenReturn(false);
        when(aiModelClient.extractDocument(eq("source.pdf"), anyString()))
                .thenThrow(new AiModelClient.AiApiException("/extract", 503));

        assertThatThrownBy(() -> worker().process(documentId))
                .isInstanceOf(AiModelClient.AiApiException.class);

        verify(persistence).markFailed(eq(documentId), any());
        verify(persistence, never()).markReady(any(), any(Integer.class));
    }

    private DocumentExtractionWorkerImpl worker() {
        var w = new DocumentExtractionWorkerImpl(
                documentRepository,
                documentObjectStorage,
                aiModelClient,
                sparseVectorGenerator,
                qdrantService,
                persistence,
                new ObjectMapper());
        ReflectionTestUtils.setField(w, "baseUrl", "http://localhost:8080");
        return w;
    }

    private static Document document(UUID id) {
        Document document = new Document();
        document.setId(id);
        document.setFileUrl("sources/raw/" + id + ".pdf");
        document.setOriginalFilename("source.pdf");
        document.setContentType("application/pdf");
        document.setDownloadToken(UUID.randomUUID().toString());
        return document;
    }

    private static DocumentChunk chunk(Document document, String text) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(document);
        chunk.setChunkIndex(0);
        chunk.setText(text);
        chunk.setActive(true);
        return chunk;
    }

    private static AiModelClient.ExtractedDocument extracted(String markdown) {
        return new AiModelClient.ExtractedDocument(
                markdown,
                List.of(new AiModelClient.ExtractionBlock("paragraph", markdown, null, null)));
    }
}
