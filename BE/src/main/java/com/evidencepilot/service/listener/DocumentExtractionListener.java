package com.evidencepilot.service.listener;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.service.DocumentExtractionWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionListener {

    private final DocumentExtractionWorker worker;

    @RabbitListener(queues = RabbitMQConfig.EXTRACTION_QUEUE)
    public void handleExtractionJob(String body) {
        UUID documentId = parseDocumentId(body);
        log.info("Received extraction job for document {}", documentId);
        worker.process(documentId);
    }

    private static UUID parseDocumentId(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Extraction job body must contain a document ID");
        }
        String normalized = body.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Extraction job body must contain a document ID");
        }
        return UUID.fromString(normalized);
    }
}
