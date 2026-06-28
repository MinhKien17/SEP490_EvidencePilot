package com.evidencepilot.service;

import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.infrastructure.AiModelClient;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionWorker {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;

    private final DocumentRepository documentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentObjectStorage documentObjectStorage;
    private final AiModelClient aiModelClient;
    private final QdrantService qdrantService;

    public void process(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));

        try {
            processDocument(document);
        } catch (RuntimeException e) {
            markFailed(document, e);
            throw e;
        }
    }

    private void processDocument(Document document) {
        byte[] raw = documentObjectStorage.read(document.getFileUrl());
        AiModelClient.ExtractedDocument extracted = aiModelClient.extractDocument(
                document.getOriginalFilename(),
                document.getContentType(),
                raw);

        List<String> chunks = chunkText(extracted.markdown());
        if (chunks.isEmpty()) {
            throw new DocumentExtractionException("Extraction produced zero chunks");
        }

        saveExtractedText(document, extracted);

        List<ExtractionResultPayload.ChunkPayload> payloadChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String text = chunks.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setText(text);
            chunk.setActive(true);
            chunk = documentChunkRepository.save(chunk);

            payloadChunks.add(new ExtractionResultPayload.ChunkPayload(
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    chunk.getText(),
                    toFloatList(aiModelClient.generateEmbedding(text))));
        }

        qdrantService.upsertVectors(new ExtractionResultPayload(document.getId(), payloadChunks));

        document.setProcessingStatus(ProcessingStatus.READY);
        document.setChunkCount(payloadChunks.size());
        document.setProcessedAt(LocalDateTime.now());
        document.setProcessingError(null);
        documentRepository.save(document);

        log.info("Completed extraction for document {} with {} chunks", document.getId(), payloadChunks.size());
    }

    private void saveExtractedText(Document document, AiModelClient.ExtractedDocument extracted) {
        DocumentText text = documentTextRepository.findByDocumentId(document.getId());
        if (text == null) {
            text = new DocumentText();
            text.setDocument(document);
        }
        text.setExtractedText(extracted.markdown());
        text.setExtractionMethod(extracted.method());
        documentTextRepository.save(text);
    }

    private void markFailed(Document document, RuntimeException exception) {
        document.setProcessingStatus(ProcessingStatus.FAILED);
        document.setProcessingError(exception.getMessage());
        document.setProcessedAt(LocalDateTime.now());
        documentRepository.save(document);
        log.warn("Failed extraction for document {}: {}", document.getId(), exception.getMessage());
    }

    private static List<String> chunkText(String text) {
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
                    int para = text.lastIndexOf("\n\n", end);
                    if (para > start + CHUNK_SIZE / 2) {
                        end = para + 2;
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
                break;
            }
            int close = text.indexOf("```", open + 3);
            if (close == -1) {
                break;
            }
            ranges.add(new int[] {open, close + 3});
            searchStart = close + 3;
        }
        return ranges;
    }

    private static int[] fenceContaining(List<int[]> fences, int start, int end) {
        for (int[] fence : fences) {
            if (start >= fence[0] && start < fence[1]) {
                return fence;
            }
            if (start < fence[0] && end > fence[0]) {
                return fence;
            }
        }
        return null;
    }

    private static List<Float> toFloatList(double[] values) {
        List<Float> floats = new ArrayList<>(values.length);
        for (double value : values) {
            floats.add((float) value);
        }
        return floats;
    }

    public static class DocumentExtractionException extends RuntimeException {
        public DocumentExtractionException(String message) {
            super(message);
        }
    }
}
