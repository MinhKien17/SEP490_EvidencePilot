package com.evidencepilot.service.listener;

import com.evidencepilot.dto.ExtractionResultMessage;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractionResultListener {

    private final DocumentRepository documentRepository;

    @RabbitListener(queues = "extraction.result.queue")
    @Transactional
    public void handleExtractionResult(ExtractionResultMessage message) {
        log.info("Received extraction result for document {}: status={}",
                message.documentId(), message.status());

        ProcessingStatus targetStatus;
        Integer chunkCount;

        if ("COMPLETED".equals(message.status())) {
            targetStatus = ProcessingStatus.READY;
            chunkCount = message.totalChunks();
        } else {
            targetStatus = ProcessingStatus.FAILED;
            chunkCount = 0;
        }

        try {
            int updatedRows = documentRepository.updateDocumentStatusAndChunks(
                    message.documentId(), targetStatus, chunkCount);

            if (updatedRows == 0) {
                log.warn("Document {} not found in database — 0 rows updated",
                        message.documentId());
            } else {
                log.info("Document {} updated to {} with chunkCount={}",
                        message.documentId(), targetStatus, chunkCount);
            }
        } catch (Exception e) {
            log.error("Failed to update document {} in database",
                    message.documentId(), e);
            throw e; // re-throw so RabbitMQ NACKs → DLQ
        }
    }
}
