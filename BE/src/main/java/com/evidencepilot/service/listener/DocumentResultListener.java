package com.evidencepilot.service.listener;

import com.evidencepilot.dto.ExtractionResultPayload;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.QdrantService;
import com.evidencepilot.service.SystemNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentResultListener {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QdrantService qdrantService;
    private final SystemNotificationService systemNotificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "extraction.result.queue")
    @Transactional
    public void handleResult(Message message) {
        ExtractionResultPayload payload;
        try {
            String rawJson = new String(message.getBody(), StandardCharsets.UTF_8);
            payload = objectMapper.readValue(rawJson, ExtractionResultPayload.class);
        } catch (Exception e) {
            log.error("Fatal JSON Mapping Error. Could not parse message body.", e);
            return;
        }

        Document document = documentRepository.findById(payload.documentId()).orElse(null);
        if (document == null) {
            log.error("Document {} not found in database", payload.documentId());
            return;
        }

        try {
            List<DocumentChunk> chunks = payload.chunks().stream()
                    .map(cp -> {
                        DocumentChunk chunk = new DocumentChunk();
                        chunk.setId(cp.chunkId());
                        chunk.setDocument(document);
                        chunk.setChunkIndex(cp.chunkIndex());
                        chunk.setText(cp.text());
                        chunk.setActive(true);
                        return chunk;
                    })
                    .toList();

            documentChunkRepository.saveAll(chunks);
            log.info("Saved {} chunks for document {}", chunks.size(), payload.documentId());

            qdrantService.upsertVectors(payload);

            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            document.setProcessedAt(LocalDateTime.now());
            documentRepository.save(document);
            notifyUploadOwner(
                    document,
                    "DOCUMENT_PROCESSING_COMPLETED",
                    "Document \"" + document.getOriginalFilename() + "\" finished processing.");
            log.info("Document {} processing completed successfully", payload.documentId());

        } catch (Exception e) {
            log.error("Failed to process extraction result for document {}", payload.documentId(), e);
            document.setProcessingStatus(ProcessingStatus.FAILED);
            document.setProcessingError(e.getMessage() != null ? e.getMessage() : "Unknown error");
            documentRepository.save(document);
            notifyUploadOwner(
                    document,
                    "DOCUMENT_PROCESSING_FAILED",
                    "Document \"" + document.getOriginalFilename() + "\" failed processing.");
        }
    }

    private void notifyUploadOwner(Document document, String actionType, String message) {
        try {
            systemNotificationService.createNotification(
                    document.getUploadedBy(),
                    null,
                    actionType,
                    document.getId(),
                    message);
        } catch (Exception notificationError) {
            log.warn("Could not notify uploader for document {}: {}",
                    document.getId(),
                    notificationError.getMessage());
        }
    }
}
