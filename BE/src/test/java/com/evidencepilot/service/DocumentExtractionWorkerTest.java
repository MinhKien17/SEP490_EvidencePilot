package com.evidencepilot.service;

import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.service.impl.DocumentExtractionWorkerImpl;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionWorkerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentTextRepository documentTextRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentObjectStorage documentObjectStorage;

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private OllamaGateway ollamaGateway;

    @Mock
    private QdrantService qdrantService;

    @Test
    void processExtractsTextChunksEmbedsAndMarksDocumentReady() {
        UUID documentId = UUID.randomUUID();
        byte[] raw = "%PDF test".getBytes();
        Document document = document(documentId);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentObjectStorage.read("sources/raw/" + documentId + ".pdf")).thenReturn(raw);
        when(aiModelClient.extractDocument("source.pdf", "application/pdf", raw))
                .thenReturn(new AiModelClient.ExtractedDocument("source.pdf", "liteparse", "First paragraph.\n\nSecond paragraph."));
        when(ollamaGateway.getEmbedding(any())).thenReturn(List.of(0.25f, -0.5f));
        when(ollamaGateway.getSparseEmbedding(any())).thenReturn(new SparseVector(List.of(0L, 1L), List.of(0.25f, -0.5f)));
        when(documentChunkRepository.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
            DocumentChunk chunk = invocation.getArgument(0);
            chunk.setId(UUID.randomUUID());
            return chunk;
        });

        new DocumentExtractionWorkerImpl(
                documentRepository,
                documentTextRepository,
                documentChunkRepository,
                documentObjectStorage,
                aiModelClient,
                ollamaGateway,
                qdrantService).process(documentId);

        ArgumentCaptor<DocumentText> textCaptor = ArgumentCaptor.forClass(DocumentText.class);
        verify(documentTextRepository).save(textCaptor.capture());
        assertThat(textCaptor.getValue().getDocument()).isSameAs(document);
        assertThat(textCaptor.getValue().getExtractedText()).isEqualTo("First paragraph.\n\nSecond paragraph.");
        assertThat(textCaptor.getValue().getExtractionMethod()).isEqualTo("liteparse");

        ArgumentCaptor<ExtractionResultPayload> payloadCaptor = ArgumentCaptor.forClass(ExtractionResultPayload.class);
        verify(qdrantService).upsertVectors(payloadCaptor.capture());
        ExtractionResultPayload payload = payloadCaptor.getValue();
        assertThat(payload.documentId()).isEqualTo(documentId);
        assertThat(payload.chunks()).hasSize(1);
        assertThat(payload.chunks().getFirst().denseEmbedding()).isEqualTo(List.of(0.25f, -0.5f));
        assertThat(payload.chunks().getFirst().sparseEmbedding()).isNotNull();
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.READY);
        assertThat(document.getChunkCount()).isEqualTo(1);
        assertThat(document.getProcessingError()).isNull();
        assertThat(document.getProcessedAt()).isNotNull();
    }

    @Test
    void processMarksDocumentFailedWhenExtractionFails() {
        UUID documentId = UUID.randomUUID();
        byte[] raw = "%PDF broken".getBytes();
        Document document = document(documentId);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentObjectStorage.read("sources/raw/" + documentId + ".pdf")).thenReturn(raw);
        when(aiModelClient.extractDocument("source.pdf", "application/pdf", raw))
                .thenThrow(new AiModelClient.AiApiException("/extract", 503, "AI offline", null));

        DocumentExtractionWorkerImpl worker = new DocumentExtractionWorkerImpl(
                documentRepository,
                documentTextRepository,
                documentChunkRepository,
                documentObjectStorage,
                aiModelClient,
                ollamaGateway,
                qdrantService);

        assertThatThrownBy(() -> worker.process(documentId))
                .isInstanceOf(AiModelClient.AiApiException.class);

        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(document.getProcessingError()).contains("AI API error on /extract");
        verify(documentRepository).save(document);
    }

    private static Document document(UUID id) {
        Document document = new Document();
        document.setId(id);
        document.setFileUrl("sources/raw/" + id + ".pdf");
        document.setOriginalFilename("source.pdf");
        document.setContentType("application/pdf");
        document.setProcessingStatus(ProcessingStatus.PROCESSING);
        return document;
    }
}
