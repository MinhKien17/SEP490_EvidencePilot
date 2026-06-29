package com.evidencepilot.service;

import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.dto.request.ClaimRequest;
import com.evidencepilot.service.impl.ClaimEvaluationServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaimEvaluationServiceImplTest {

    @Test
    void evaluateRequiresDocumentAccessBeforeVectorSearch() {
        QdrantGateway qdrantGateway = mock(QdrantGateway.class);
        OllamaGateway ollamaGateway = mock(OllamaGateway.class);
        DocumentService documentService = mock(DocumentService.class);
        UUID documentId = UUID.randomUUID();

        when(ollamaGateway.getEmbedding("claim")).thenReturn(List.of(0.1f));
        when(ollamaGateway.getSparseEmbedding("claim"))
                .thenReturn(new SparseVector(List.of(1L), List.of(0.2f)));
        when(qdrantGateway.searchDocumentContext(eq(documentId), any(), any(), eq(20)))
                .thenReturn(List.of());

        new ClaimEvaluationServiceImpl(qdrantGateway, ollamaGateway, documentService)
                .evaluate(documentId, new ClaimRequest("claim"));

        var inOrder = inOrder(documentService, qdrantGateway);
        inOrder.verify(documentService).getDocumentById(documentId);
        inOrder.verify(qdrantGateway).searchDocumentContext(eq(documentId), any(), any(), eq(20));
    }
}
