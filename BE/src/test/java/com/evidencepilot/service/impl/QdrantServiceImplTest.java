package com.evidencepilot.service.impl;

import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.model.Document;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.QdrantClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QdrantServiceImplTest {

    @Test
    void upsertVectorsRejectsEmptyDenseEmbedding() {
        QdrantClient qdrantClient = mock(QdrantClient.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(new Document()));

        QdrantServiceImpl service = new QdrantServiceImpl(qdrantClient, documentRepository);
        ExtractionResultPayload payload = new ExtractionResultPayload(
                documentId,
                List.of(new ExtractionResultPayload.ChunkPayload(
                        chunkId,
                        0,
                        "text",
                        List.of(),
                        null)));

        assertThatThrownBy(() -> service.upsertVectors(payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(chunkId.toString());
        verifyNoInteractions(qdrantClient);
    }
}
