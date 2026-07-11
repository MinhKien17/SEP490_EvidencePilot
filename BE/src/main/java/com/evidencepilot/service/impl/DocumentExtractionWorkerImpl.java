package com.evidencepilot.service.impl;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.dto.EmbeddingManifest;
import com.evidencepilot.dto.EmbeddingOutput;
import com.evidencepilot.dto.EmbeddingRequest;
import com.evidencepilot.dto.EmbeddingResult;
import com.evidencepilot.dto.ExtractionResult;
import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.service.DocumentObjectStorage;
import com.evidencepilot.service.QdrantClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionWorkerImpl {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;
    private static final int UPSERT_BATCH_SIZE = 256;

    private final DocumentRepository documentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentObjectStorage documentObjectStorage;
    private final SparseVectorGenerator sparseVectorGenerator;
    private final QdrantClient qdrantClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.EXTRACTION_RESULT_QUEUE)
    public void processExtractionResult(ExtractionResult result) {
        Document document = document(result.documentId());
        if ("FAILED".equals(result.status())) {
            markFailed(document, result.error());
            return;
        }
        if (!"EXTRACTED".equals(result.status())) {
            throw new DocumentExtractionException("Unsupported extraction status " + result.status());
        }

        byte[] markdownBytes = documentObjectStorage.read(result.markdownObjectKey());
        if (!sha256(markdownBytes).equalsIgnoreCase(result.sha256())) {
            throw new DocumentExtractionException("Extracted Markdown checksum mismatch");
        }
        String markdown = new String(markdownBytes, StandardCharsets.UTF_8);
        List<String> texts = chunkText(markdown);
        if (texts.isEmpty()) {
            throw new DocumentExtractionException("Extraction produced zero chunks");
        }

        saveExtractedText(document, markdown, result.extractionMethod());
        List<DocumentChunk> oldChunks = documentChunkRepository
                .findByDocumentIdOrderByChunkIndexAsc(document.getId());
        List<DocumentChunk> activeChunks = new ArrayList<>(texts.size());
        for (int index = 0; index < texts.size(); index++) {
            DocumentChunk chunk = index < oldChunks.size() ? oldChunks.get(index) : new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(index);
            chunk.setText(texts.get(index));
            chunk.setActive(true);
            activeChunks.add(documentChunkRepository.save(chunk));
        }

        for (int index = texts.size(); index < oldChunks.size(); index++) {
            DocumentChunk stale = oldChunks.get(index);
            stale.setActive(false);
            documentChunkRepository.save(stale);
        }

        UUID jobId = result.sha256().equals(document.getExtractionHashSha256())
                && document.getEmbeddingJobId() != null
                ? document.getEmbeddingJobId()
                : UUID.randomUUID();
        document.setExtractionHashSha256(result.sha256());
        document.setEmbeddingJobId(jobId);
        document.setProcessingStatus(ProcessingStatus.PROCESSING);
        document.setChunkCount(activeChunks.size());
        document.setProcessingError(null);
        documentRepository.save(document);

        EmbeddingManifest manifest = new EmbeddingManifest(
                jobId,
                document.getId(),
                activeChunks.stream()
                        .map(chunk -> new EmbeddingManifest.Chunk(
                                chunk.getId(), chunk.getChunkIndex(), chunk.getText()))
                        .toList());
        String manifestKey = "embedding-jobs/" + document.getId() + "/" + jobId + ".json";
        documentObjectStorage.write(manifestKey, json(manifest), "application/json");
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_EMBEDDING_REQUEST,
                new EmbeddingRequest("DOCUMENT_BATCH", jobId, document.getId(), manifestKey),
                message -> {
                    message.getMessageProperties().setPriority(1);
                    return message;
                });
        log.info("Queued {} chunks for embedding for document {}", activeChunks.size(), document.getId());
    }

    @RabbitListener(queues = RabbitMQConfig.EMBEDDING_RESULT_QUEUE)
    public void processEmbeddingResult(EmbeddingResult result) {
        Document document = document(result.documentId());
        if (!result.jobId().equals(document.getEmbeddingJobId())) {
            log.info("Ignoring stale embedding job {} for document {}", result.jobId(), document.getId());
            return;
        }
        if ("FAILED".equals(result.status())) {
            markFailed(document, result.error());
            return;
        }
        if (!"EMBEDDED".equals(result.status())) {
            throw new DocumentExtractionException("Unsupported embedding status " + result.status());
        }

        EmbeddingOutput output = readJson(result.resultObjectKey(), EmbeddingOutput.class);
        if (!result.jobId().equals(output.jobId()) || !document.getId().equals(output.documentId())) {
            throw new DocumentExtractionException("Embedding result identity mismatch");
        }

        List<DocumentChunk> allChunks = documentChunkRepository
                .findByDocumentIdOrderByChunkIndexAsc(document.getId());
        List<DocumentChunk> chunks = allChunks.stream()
                .filter(DocumentChunk::isActive)
                .toList();
        Map<UUID, List<Float>> vectors = new HashMap<>();
        for (EmbeddingOutput.Item item : output.embeddings()) {
            vectors.put(item.chunkId(), item.embedding());
        }
        if (vectors.size() != chunks.size()) {
            throw new DocumentExtractionException("Embedding result chunk count mismatch");
        }

        String projectId = document.getProject() == null
                ? "0"
                : document.getProject().getId().toString();
        List<QdrantClient.VectorPoint> points = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            List<Float> dense = vectors.get(chunk.getId());
            if (dense == null || dense.isEmpty()) {
                throw new DocumentExtractionException("Missing embedding for chunk " + chunk.getId());
            }
            SparseVector sparse = sparseVectorGenerator.generate(chunk.getText());
            points.add(new QdrantClient.VectorPoint(
                    chunk.getId().toString(),
                    dense,
                    sparse,
                    "PROJECT",
                    projectId,
                    Map.of(
                            "document_id", document.getId().toString(),
                            "chunk_id", chunk.getId().toString(),
                            "chunk_index", chunk.getChunkIndex(),
                            "text", chunk.getText())));
        }
        for (int start = 0; start < points.size(); start += UPSERT_BATCH_SIZE) {
            qdrantClient.upsertVectors(points.subList(
                    start, Math.min(start + UPSERT_BATCH_SIZE, points.size())));
        }
        List<UUID> stalePointIds = new ArrayList<>();
        stalePointIds.add(document.getId()); // Remove the legacy one-point-per-document vector.
        allChunks.stream().filter(chunk -> !chunk.isActive()).map(DocumentChunk::getId).forEach(stalePointIds::add);
        qdrantClient.deleteVectors(stalePointIds.stream().map(UUID::toString).toList());

        document.setProcessingStatus(ProcessingStatus.READY);
        document.setProcessedAt(LocalDateTime.now());
        document.setProcessingError(null);
        document.setEmbeddingJobId(null);
        documentRepository.save(document);
        log.info("Indexed document {} with {} chunks", document.getId(), chunks.size());
    }

    private Document document(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
    }

    private void saveExtractedText(Document document, String markdown, String method) {
        DocumentText text = documentTextRepository.findByDocumentId(document.getId());
        if (text == null) {
            text = new DocumentText();
            text.setDocument(document);
        }
        text.setExtractedText(markdown);
        text.setExtractionMethod(method);
        documentTextRepository.save(text);
    }

    private void markFailed(Document document, String error) {
        document.setProcessingStatus(ProcessingStatus.FAILED);
        document.setProcessingError(error == null ? "Model worker failed" : error);
        document.setProcessedAt(LocalDateTime.now());
        documentRepository.save(document);
        log.warn("Failed document {}: {}", document.getId(), document.getProcessingError());
    }

    private <T> T readJson(String objectKey, Class<T> type) {
        try {
            return objectMapper.readValue(documentObjectStorage.read(objectKey), type);
        } catch (Exception e) {
            throw new DocumentExtractionException("Invalid JSON artifact " + objectKey, e);
        }
    }

    private byte[] json(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new DocumentExtractionException("Failed to serialize embedding manifest", e);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
            if (open == -1) break;
            int close = text.indexOf("```", open + 3);
            if (close == -1) break;
            ranges.add(new int[]{open, close + 3});
            searchStart = close + 3;
        }
        return ranges;
    }

    private static int[] fenceContaining(List<int[]> fences, int start, int end) {
        for (int[] fence : fences) {
            if (start >= fence[0] && start < fence[1]) return fence;
            if (start < fence[0] && end > fence[0]) return fence;
        }
        return null;
    }

    public static class DocumentExtractionException extends RuntimeException {
        public DocumentExtractionException(String message) {
            super(message);
        }

        public DocumentExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
