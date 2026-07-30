package com.evidencepilot.repository;

import com.evidencepilot.model.AiSuggestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {
    List<AiSuggestion> findByClaimId(UUID claimId);

    @EntityGraph(attributePaths = {"documentChunk", "documentChunk.document"})
    List<AiSuggestion> findByClaimIdAndClaimVersionOrderByCreatedAtDesc(
            UUID claimId,
            Integer claimVersion);

    @EntityGraph(attributePaths = {"documentChunk", "documentChunk.document"})
    Optional<AiSuggestion> findFirstByClaimIdAndClaimVersionAndDocumentChunkIdOrderByCreatedAtDesc(
            UUID claimId,
            Integer claimVersion,
            UUID documentChunkId);

    List<AiSuggestion> findByDocumentChunkId(UUID documentChunkId);
    List<AiSuggestion> findByStatusOrderByCreatedAtDesc(String status);
}
