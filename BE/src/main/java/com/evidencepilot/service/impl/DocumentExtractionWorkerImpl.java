package com.evidencepilot.service.impl;

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

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;
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
        String markdownKey = "documents/processed/" + document.getId() + "/document.md";
        AiModelClient.ExtractedDocument extracted;
        if (documentObjectStorage.exists(markdownKey)) {
            extracted = new AiModelClient.ExtractedDocument(
                    document.getOriginalFilename(),
                    extractionMethod(document.getOriginalFilename()),
                    documentObjectStorage.readText(markdownKey));
        } else {
            String downloadUrl = baseUrl + "/api/documents/" + document.getId()
                    + "/download?token=" + document.getDownloadToken();
            extracted = aiModelClient.extractDocument(
                    document.getId(),
                    document.getOriginalFilename(),
                    document.getContentType(),
                    downloadUrl);
            documentObjectStorage.writeText(markdownKey, extracted.markdown());
        }

        List<String> chunks = chunkText(extracted.markdown());
        if (chunks.isEmpty()) {
            throw new DocumentExtractionException("Extraction produced zero chunks");
        }
        List<List<Float>> dense = embed(chunks);
        List<SparseVector> sparse = chunks.stream()
                .map(sparseVectorGenerator::generate)
                .toList();
        List<DocumentChunk> savedChunks = documentPersistenceService.saveExtraction(
                document.getId(), extracted.method(), extracted.markdown(), chunks);
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

    private static String extractionMethod(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".docx") ? "liteparse" : "mineru";
    }

    static List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (text.length() <= CHUNK_SIZE) {
            return List.of(text);
        }

        List<int[]> fences = codeFenceRanges(text);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int[] fence = fenceContaining(fences, start, end);
                if (fence != null) {
                    end = Math.min(fence[1], text.length());
                } else {
                    int paragraph = text.lastIndexOf("\n\n", end);
                    if (paragraph > start + CHUNK_SIZE / 2) {
                        end = paragraph + 2;
                    } else {
                        int newline = text.lastIndexOf('\n', end);
                        if (newline > start + CHUNK_SIZE / 2) {
                            end = newline + 1;
                        }
                    }
                }
            }
            chunks.add(text.substring(start, end));
            start = end < text.length() ? Math.max(end - CHUNK_OVERLAP, start + 1) : end;
        }
        return chunks;
    }

    private static List<int[]> codeFenceRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        int searchStart = 0;
        while (true) {
            int open = text.indexOf("```", searchStart);
            if (open == -1) {
                return ranges;
            }
            int close = text.indexOf("```", open + 3);
            if (close == -1) {
                return ranges;
            }
            ranges.add(new int[] {open, close + 3});
            searchStart = close + 3;
        }
    }

    private static int[] fenceContaining(List<int[]> fences, int start, int end) {
        for (int[] fence : fences) {
            if ((start >= fence[0] && start < fence[1])
                    || (start < fence[0] && end > fence[0])) {
                return fence;
            }
        }
        return null;
    }
}
