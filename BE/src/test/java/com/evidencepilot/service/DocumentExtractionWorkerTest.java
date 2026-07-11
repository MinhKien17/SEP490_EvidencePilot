package com.evidencepilot.service;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.dto.EmbeddingOutput;
import com.evidencepilot.dto.EmbeddingRequest;
import com.evidencepilot.dto.EmbeddingResult;
import com.evidencepilot.dto.ExtractionResult;
import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.service.impl.DocumentExtractionWorkerImpl;
import com.evidencepilot.service.impl.SparseVectorGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionWorkerTest {

    @Mock DocumentRepository documents;
    @Mock DocumentTextRepository texts;
    @Mock DocumentChunkRepository chunks;
    @Mock DocumentObjectStorage storage;
    @Mock SparseVectorGenerator sparse;
    @Mock QdrantClient qdrant;
    @Mock RabbitTemplate rabbit;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractionResultCreatesChunksAndQueuesEmbeddingManifest() throws Exception {
        UUID documentId = UUID.randomUUID();
        Document document = document(documentId);
        String markdown = "A".repeat(900) + "\n\n" + "B".repeat(900);
        byte[] markdownBytes = markdown.getBytes(StandardCharsets.UTF_8);
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(markdownBytes));
        when(documents.findById(documentId)).thenReturn(Optional.of(document));
        when(storage.read("extractions/document.md")).thenReturn(markdownBytes);
        when(chunks.findByDocumentIdOrderByChunkIndexAsc(documentId)).thenReturn(List.of());
        when(chunks.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
            DocumentChunk chunk = invocation.getArgument(0);
            if (chunk.getId() == null) chunk.setId(UUID.randomUUID());
            return chunk;
        });

        worker().processExtractionResult(new ExtractionResult(
                documentId, "EXTRACTED", "extractions/document.md", checksum, "mineru", null));

        ArgumentCaptor<DocumentText> text = ArgumentCaptor.forClass(DocumentText.class);
        verify(texts).save(text.capture());
        assertThat(text.getValue().getExtractedText()).isEqualTo(markdown);
        assertThat(document.getChunkCount()).isGreaterThan(1);
        assertThat(document.getEmbeddingJobId()).isNotNull();
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSING);
        verify(storage).write(
                org.mockito.ArgumentMatchers.startsWith("embedding-jobs/"),
                any(byte[].class),
                eq("application/json"));
        verify(rabbit).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY_EMBEDDING_REQUEST),
                any(EmbeddingRequest.class),
                any(MessagePostProcessor.class));
    }

    @Test
    void embeddingResultIndexesDatabaseChunksAndMarksReady() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Document document = document(documentId);
        document.setEmbeddingJobId(jobId);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(document);
        chunk.setChunkIndex(0);
        chunk.setText("Evidence text");
        chunk.setActive(true);
        EmbeddingOutput output = new EmbeddingOutput(
                jobId,
                documentId,
                List.of(new EmbeddingOutput.Item(chunk.getId(), List.of(0.2f, -0.1f))));

        when(documents.findById(documentId)).thenReturn(Optional.of(document));
        when(chunks.findByDocumentIdOrderByChunkIndexAsc(documentId)).thenReturn(List.of(chunk));
        when(storage.read("embedding-results/job.json")).thenReturn(objectMapper.writeValueAsBytes(output));
        when(sparse.generate("Evidence text"))
                .thenReturn(new SparseVector(List.of(1L), List.of(1f)));

        worker().processEmbeddingResult(new EmbeddingResult(
                jobId, documentId, "EMBEDDED", "embedding-results/job.json", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QdrantClient.VectorPoint>> points = ArgumentCaptor.forClass(List.class);
        verify(qdrant).upsertVectors(points.capture());
        assertThat(points.getValue()).singleElement().satisfies(indexed -> {
            assertThat(indexed.chunkId()).isEqualTo(chunk.getId().toString());
            assertThat(indexed.denseVector()).containsExactly(0.2f, -0.1f);
        });
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.READY);
        assertThat(document.getEmbeddingJobId()).isNull();
    }

    private DocumentExtractionWorkerImpl worker() {
        DocumentExtractionWorkerImpl worker = new DocumentExtractionWorkerImpl(
                documents, texts, chunks, storage, sparse, qdrant, rabbit, objectMapper);
        return worker;
    }

    private static Document document(UUID id) {
        Document document = new Document();
        document.setId(id);
        document.setProcessingStatus(ProcessingStatus.PROCESSING);
        return document;
    }
}
