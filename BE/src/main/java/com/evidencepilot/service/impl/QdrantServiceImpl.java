package com.evidencepilot.service.impl;

import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.service.QdrantClient;
import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantServiceImpl implements QdrantService {

    private final QdrantClient qdrantClient;
    private final DocumentRepository documentRepository;
    private final ProjectDocumentRepository projectDocumentRepository;

    @Override
    public void upsertVectors(ExtractionResultPayload payload) {
        Document document = documentRepository.findById(payload.documentId()).orElse(null);
        if (document == null) {
            log.warn("Document {} not found, skipping Qdrant upsert", payload.documentId());
            return;
        }

        List<String> projectIds;
        boolean multiProject;
        if (document.getProject() != null) {
            projectIds = List.of(document.getProject().getId().toString());
            multiProject = false;
        } else {
            projectIds = projectDocumentRepository.findByDocumentId(document.getId()).stream()
                    .map(pd -> pd.getProject().getId().toString())
                    .distinct()
                    .toList();
            if (projectIds.isEmpty()) {
                projectIds = List.of("0");
            }
            multiProject = projectIds.size() > 1;
        }

        int upserted = 0;
        for (ExtractionResultPayload.ChunkPayload chunk : payload.chunks()) {
            if (chunk.denseEmbedding() == null || chunk.denseEmbedding().isEmpty()) {
                throw new IllegalStateException("Chunk " + chunk.chunkId() + " has empty dense embedding");
            }
            for (String projectId : projectIds) {
                String pointId = multiProject
                        ? chunk.chunkId().toString() + "_" + projectId
                        : chunk.chunkId().toString();
                qdrantClient.upsertVector(
                        pointId,
                        chunk.denseEmbedding(),
                        chunk.sparseEmbedding(),
                        "PROJECT",
                        projectId,
                        Map.ofEntries(
                                entry("document_id", payload.documentId().toString()),
                                entry("chunk_id", chunk.chunkId().toString()),
                                entry("chunk_index", chunk.chunkIndex()),
                                entry("text", chunk.text())
                        )
                );
                upserted++;
            }
        }
        log.info("Upserted {} vectors to Qdrant for document {} (projects: {})",
                upserted, payload.documentId(), projectIds);
    }
}
