package com.evidencepilot.service;

import com.evidencepilot.client.qdrant.QdrantSearchRequest;
import com.evidencepilot.client.qdrant.QdrantSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class QdrantGateway {

    private final RestClient restClient;

    public QdrantGateway(@Value("${qdrant.url:http://localhost:6333}") String qdrantUrl) {
        this.restClient = RestClient.builder().baseUrl(qdrantUrl).build();
    }

    public List<String> searchDocumentContext(UUID documentId, List<Float> queryVector, int topK) {
        log.info("Searching Qdrant for document {} with topK={}", documentId, topK);

        QdrantSearchRequest request = QdrantSearchRequest.forDocument(
                documentId.toString(),
                queryVector,
                topK
        );

        QdrantSearchResponse response = restClient.post()
                .uri("/collections/evidence_chunks/points/search")
                .body(request)
                .retrieve()
                .body(QdrantSearchResponse.class);

        if (response == null || response.result() == null || response.result().isEmpty()) {
            log.warn("Qdrant search returned 0 chunks for document {}", documentId);
            return List.of();
        }

        List<String> texts = response.result().stream()
                .map(QdrantSearchResponse.ScoredPoint::getText)
                .filter(t -> !t.isBlank())
                .toList();

        log.info("Qdrant search returned {} text chunks for document {}", texts.size(), documentId);
        return texts;
    }
}
