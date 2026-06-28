package com.evidencepilot.service.impl;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.SourceExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceExtractionServiceImpl implements SourceExtractionService {

    private final DocumentRepository documentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public void triggerExtraction(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));

        doc.setProcessingStatus(ProcessingStatus.PROCESSING);
        documentRepository.save(doc);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXTRACTION_QUEUE, documentId.toString());
        log.info("Published document {} to extraction.queue", documentId);
    }

    @Override
    public ExtractedText extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String method = "unknown";
        String text;

        try {
            if ("application/pdf".equals(contentType) || (filename != null && filename.toLowerCase().endsWith(".pdf"))) {
                text = extractPdfText(file);
                method = "pdfbox";
            } else {
                text = new String(file.getBytes());
                method = "raw";
            }
        } catch (IOException e) {
            log.warn("Text extraction failed for file {}: {}", filename, e.getMessage());
            text = "";
            method = "failed";
        }

        return new ExtractedText(text, method);
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String raw = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (!raw.isBlank() && raw.contains(" ")) {
            return raw;
        }
        return "PDF content could not be extracted (pdfbox not available).";
    }
}
