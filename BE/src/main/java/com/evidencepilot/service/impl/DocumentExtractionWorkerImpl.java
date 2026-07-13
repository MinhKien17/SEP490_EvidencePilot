package com.evidencepilot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.DocumentExtractionWorker;
import com.evidencepilot.service.DocumentObjectStorage;
import com.evidencepilot.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionWorkerImpl implements DocumentExtractionWorker {

    private static final int EMBEDDING_BATCH_SIZE = 32;
    private static final int EMBEDDING_DIMENSION = 768;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final DocumentRepository documentRepository;
    private final DocumentObjectStorage documentObjectStorage;
    private final AiModelClient aiModelClient;
    private final SparseVectorGenerator sparseVectorGenerator;
    private final QdrantService qdrantService;
    private final DocumentPersistenceService documentPersistenceService;
    private final ObjectMapper objectMapper;

    @Override
    public void process(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
        documentPersistenceService.markProcessing(documentId);
        try {
            processDocument(document);
        } catch (RuntimeException e) {
            documentPersistenceService.markFailed(documentId, e.getMessage());
            throw e;
        }
    }

    private void processDocument(Document document) {
        String checkpointKey = "documents/processed/" + document.getId() + "/extraction.json";
        AiModelClient.ExtractedDocument extracted = readCheckpoint(checkpointKey);
        if (extracted == null) {
            String downloadUrl = baseUrl + "/api/documents/" + document.getId()
                    + "/download?token=" + document.getDownloadToken();
            extracted = aiModelClient.extractDocument(
                    document.getOriginalFilename(),
                    downloadUrl);
            if (!extracted.valid()) {
                throw new DocumentExtractionException("Extraction returned an invalid document");
            }
            writeCheckpoint(checkpointKey, extracted);
        }

        List<String> chunks = DocumentChunker.chunk(extracted.blocks());
        if (chunks.isEmpty()) {
            throw new DocumentExtractionException("Extraction produced zero chunks");
        }
        List<List<Float>> dense = embed(chunks);
        List<SparseVector> sparse = chunks.stream()
                .map(sparseVectorGenerator::generate)
                .toList();
        List<DocumentChunk> savedChunks = documentPersistenceService.saveExtraction(
                document.getId(), "mineru", extracted.markdown(), chunks);
        if (savedChunks.size() != chunks.size()) {
            throw new DocumentExtractionException("Failed to persist every document chunk");
        }

        List<ExtractionResultPayload.ChunkPayload> payloadChunks = new ArrayList<>();
        for (int index = 0; index < savedChunks.size(); index++) {
            DocumentChunk chunk = savedChunks.get(index);
            payloadChunks.add(new ExtractionResultPayload.ChunkPayload(
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    chunk.getText(),
                    dense.get(index),
                    sparse.get(index)));
        }

        qdrantService.upsertVectors(new ExtractionResultPayload(document.getId(), payloadChunks));
        documentPersistenceService.markReady(document.getId(), payloadChunks.size());
        log.info("Completed extraction for document {} with {} chunks", document.getId(), payloadChunks.size());
    }

    private AiModelClient.ExtractedDocument readCheckpoint(String checkpointKey) {
        if (!documentObjectStorage.exists(checkpointKey)) {
            return null;
        }
        try {
            AiModelClient.ExtractedDocument extracted = objectMapper.readValue(
                    documentObjectStorage.readText(checkpointKey),
                    AiModelClient.ExtractedDocument.class);
            if (extracted != null && extracted.valid()) {
                return extracted;
            }
        } catch (JsonProcessingException e) {
            log.warn("Ignoring invalid extraction checkpoint {}", checkpointKey, e);
        }
        return null;
    }

    private void writeCheckpoint(String checkpointKey, AiModelClient.ExtractedDocument extracted) {
        try {
            documentObjectStorage.write(
                    checkpointKey,
                    objectMapper.writeValueAsBytes(extracted),
                    "application/json");
        } catch (JsonProcessingException e) {
            throw new DocumentExtractionException("Failed to serialize extraction checkpoint: " + e.getMessage());
        }
    }

    private List<List<Float>> embed(List<String> chunks) {
        List<List<Float>> embeddings = new ArrayList<>();
        for (int start = 0; start < chunks.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, chunks.size());
            List<List<Float>> batch = aiModelClient.generateEmbeddings(chunks.subList(start, end));
            if (batch.size() != end - start) {
                throw new DocumentExtractionException("Embedding count does not match chunk count");
            }
            for (List<Float> vector : batch) {
                if (vector.size() != EMBEDDING_DIMENSION) {
                    throw new DocumentExtractionException("Embedding dimension must be " + EMBEDDING_DIMENSION);
                }
            }
            embeddings.addAll(batch);
        }
        return embeddings;
    }

}
