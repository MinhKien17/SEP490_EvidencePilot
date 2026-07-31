package com.evidencepilot.service;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.PaperValidationResponse;
import com.evidencepilot.dto.response.AiReviewResponse;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface PaperProcessingService {

    List<PaperSectionResponse> getPaperSections(UUID documentId);

    List<PaperSectionResponse> getPaperSectionsByUser(UUID documentId, UUID userId);

    PaperSectionResponse getSectionHistory(UUID documentId, UUID sectionId);

    List<PaperSectionResponse> detectAndPersistSections(UUID documentId);

    AiReviewResponse review(UUID documentId, String targetStyle);

    PaperValidationResponse validateSections(UUID documentId);

    PaperSectionResponse updateSection(UUID documentId, UUID sectionId, String title, Integer order, UUID mergeIntoId, String content);

    PaperSectionResponse createSection(UUID documentId, String title, UUID parentSectionId);

    List<PaperSectionResponse> createSectionsFromStandard(UUID documentId, String standard);

    /**
     * Atomically resets the sections of the project's single paper to match the given standard.
     * If no paper exists yet, creates a stub paper first (same behaviour as /papers/init).
     * Throws 409 CONFLICT if any section is currently assigned to a student.
     */
    List<PaperSectionResponse> resetSectionsForStandard(UUID projectId, String standard);

    PaperSectionResponse assignSection(UUID documentId, UUID sectionId, UUID assignedUserId);

    PaperSectionResponse rollbackSection(UUID documentId, UUID sectionId);

    void deleteSection(UUID documentId, UUID sectionId);

    Path exportTexArchive(UUID projectId);
}
