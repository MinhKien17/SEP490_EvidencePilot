package com.evidencepilot.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface SourceExtractionService {

    void triggerExtraction(UUID documentId);

    ExtractedText extractText(MultipartFile file);

    record ExtractedText(String text, String method) {}
}
