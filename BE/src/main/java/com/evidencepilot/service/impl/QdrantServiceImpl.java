package com.evidencepilot.service.impl;

import com.evidencepilot.client.qdrant.QdrantClient;
import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.model.Document;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantServiceImpl implements QdrantService {

    private final QdrantClient qdrantClient;
    private final DocumentRepository documentRepository;

    @Override
    public void upsertVectors(ExtractionResultPayload payload) {
        Document document = documentRepository.findById(payload.documentId()).orElse(null);
        if (document == null) {
            log.warn("Document {} not found, skipping Qdrant upsert", payload.documentId());
            return;
        }
        String projectId = document.getProject() != null
                ? document.getProject().getId().toString()
                : "0";

        for (ExtractionResultPayload.ChunkPayload chunk : payload.chunks()) {
            if (chunk.embedding() == null || chunk.embedding().isEmpty()) {
                log.debug("Skipping chunk {} with empty embedding", chunk.chunkId());
                continue;
            }
            qdrantClient.upsertVector(
                    chunk.chunkId().toString(),
                    chunk.embedding(),
                    "PROJECT",
                    projectId
            );
        }
        log.info("Upserted {} vectors to Qdrant for document {}",
                payload.chunks().size(), payload.documentId());
    }
}
