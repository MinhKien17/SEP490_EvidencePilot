package com.evidencepilot.service;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.PaperValidationResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PaperProcessingService {

    List<PaperSectionResponse> getPaperSections(UUID documentId);

    List<PaperSectionResponse> detectAndPersistSections(UUID documentId);

    Map<String, Object> review(UUID documentId, String targetStyle);

    PaperValidationResponse validateSections(UUID documentId);

    PaperSectionResponse updateSection(UUID documentId, UUID sectionId, String title, Integer order, UUID mergeIntoId);

    PaperSectionResponse createSection(UUID documentId, String title, UUID parentSectionId);
}
