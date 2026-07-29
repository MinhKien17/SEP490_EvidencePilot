package com.evidencepilot.repository;

import com.evidencepilot.model.SectionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface SectionFeedbackRepository extends JpaRepository<SectionFeedback, UUID> {
    List<SectionFeedback> findBySectionId(UUID sectionId);
    List<SectionFeedback> findByAuthorId(UUID authorId);
    List<SectionFeedback> findBySectionIdAndAuthorId(UUID sectionId, UUID authorId);
    // Bulk DELETE WHERE section_id IN (...) — must run BEFORE paperSectionRepository.deleteByDocumentId
    // to satisfy the NOT NULL FK constraint on section_feedback.section_id.
    @Transactional
    void deleteAllBySectionIdIn(List<UUID> sectionIds);
}
