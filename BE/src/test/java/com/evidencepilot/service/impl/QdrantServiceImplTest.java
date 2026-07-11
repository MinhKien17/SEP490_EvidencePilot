package com.evidencepilot.service.impl;

import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.QdrantClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QdrantServiceImplTest {

    @Test
    void upsertVectorsWritesEachChunkWithinProjectScope() {
        QdrantClient client = mock(QdrantClient.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        Project project = new Project();
        project.setId(UUID.randomUUID());
        Document document = new Document();
        document.setProject(project);
        when(documents.findById(documentId)).thenReturn(Optional.of(document));
        var chunk = new ExtractionResultPayload.ChunkPayload(
                chunkId, 1, "text", List.of(0.2f), null);

        new QdrantServiceImpl(client, documents)
                .upsertVectors(new ExtractionResultPayload(documentId, List.of(chunk)));

        verify(client).upsertVector(
                org.mockito.ArgumentMatchers.eq(chunkId.toString()),
                org.mockito.ArgumentMatchers.eq(List.of(0.2f)),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("PROJECT"),
                org.mockito.ArgumentMatchers.eq(project.getId().toString()),
                org.mockito.ArgumentMatchers.argThat(payload -> payload.get("document_id").equals(documentId.toString())));
    }

    @Test
    void upsertVectorsSkipsMissingDocument() {
        QdrantClient client = mock(QdrantClient.class);
        DocumentRepository documents = mock(DocumentRepository.class);

        new QdrantServiceImpl(client, documents)
                .upsertVectors(new ExtractionResultPayload(UUID.randomUUID(), List.of()));

        verifyNoInteractions(client);
    }

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
