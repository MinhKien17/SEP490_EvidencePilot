package com.evidencepilot.service.impl;

import com.evidencepilot.dto.QdrantSearchRequest;
import com.evidencepilot.dto.QdrantSearchResponse;
import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.service.QdrantGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class QdrantGatewayImpl implements QdrantGateway {

    private static final float SCORE_THRESHOLD = 0.0f;

    private final RestClient restClient;

    public QdrantGatewayImpl(@Value("${qdrant.url:http://localhost:6333}") String qdrantUrl) {
        this.restClient = RestClient.builder().baseUrl(qdrantUrl).build();
    }

    @Override
    public List<String> searchDocumentContext(UUID documentId, List<Float> denseVector, SparseVector sparseVector, int topK) {
        log.info("Searching Qdrant for document {} with topK={}", documentId, topK);

        QdrantSearchRequest request = QdrantSearchRequest.forDocument(
                documentId.toString(),
                denseVector,
                sparseVector,
                topK
        );

        QdrantSearchResponse response = restClient.post()
                .uri("/collections/source_chunks/points/query")
                .body(request)
                .retrieve()
                .body(QdrantSearchResponse.class);

        if (response == null || response.result() == null || response.result().points() == null || response.result().points().isEmpty()) {
            log.warn("Qdrant search returned 0 chunks for document {}", documentId);
            return List.of();
        }

        List<String> texts = response.result().points().stream()
                .filter(point -> point.score() > SCORE_THRESHOLD)
                .map(QdrantSearchResponse.ScoredPoint::getText)
                .filter(t -> !t.isBlank())
                .toList();

        if (texts.isEmpty()) {
            log.warn("Qdrant search returned 0 chunks after score threshold for document {}", documentId);
            return List.of();
        }

        log.info("Qdrant search returned {} text chunks for document {}", texts.size(), documentId);
        return texts;
    }
}
