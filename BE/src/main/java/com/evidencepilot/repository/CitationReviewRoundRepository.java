package com.evidencepilot.repository;

import com.evidencepilot.model.CitationReviewRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CitationReviewRoundRepository extends JpaRepository<CitationReviewRound, UUID> {
    boolean existsBySectionIdAndContentFingerprint(UUID sectionId, String contentFingerprint);

    List<CitationReviewRound> findBySectionIdOrderByCreatedAtDesc(UUID sectionId);
}
