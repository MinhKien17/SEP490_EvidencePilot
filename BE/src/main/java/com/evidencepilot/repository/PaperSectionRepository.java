package com.evidencepilot.repository;

import com.evidencepilot.model.PaperSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaperSectionRepository extends JpaRepository<PaperSection, UUID> {
    List<PaperSection> findByDocumentIdOrderBySectionOrderAsc(UUID documentId);
    List<PaperSection> findByDocumentIdAndAssignedUserIdOrderBySectionOrderAsc(UUID documentId, UUID assignedUserId);
    Optional<PaperSection> findByIdAndAssignedUserId(UUID id, UUID assignedUserId);
    // Bulk hard-delete all sections for a paper — used by resetSectionsForStandard.
    // Spring Data derives: DELETE FROM paper_sections WHERE document_id = ?
    // @Transactional is required by Spring Data for derived-delete methods.
    @Transactional
    void deleteByDocumentId(UUID documentId);
}
