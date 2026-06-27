package com.evidencepilot.service;

import com.evidencepilot.model.Source;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface SourceExtractionService {

    void triggerExtraction(UUID documentId);

    void extractAndPersist(Source source, MultipartFile file);

    ExtractedText extractText(MultipartFile file);

    record ExtractedText(String text, String method) {}
}
