package com.evidencepilot.repository;

import com.evidencepilot.model.PaperSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaperSectionRepository extends JpaRepository<PaperSection, UUID> {
    List<PaperSection> findByDocumentIdOrderBySectionOrderAsc(UUID documentId);
    List<PaperSection> findByDocumentIdAndAssignedUserIdOrderBySectionOrderAsc(UUID documentId, UUID assignedUserId);
    Optional<PaperSection> findByIdAndAssignedUserId(UUID id, UUID assignedUserId);
}
