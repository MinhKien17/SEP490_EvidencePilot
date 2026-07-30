package com.evidencepilot.repository;

import com.evidencepilot.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
    List<DocumentChunk> findByDocumentId(UUID documentId);

    @Query("""
            select c from DocumentChunk c
            join fetch c.document d
            left join fetch d.project
            where c.id = :id
            """)
    Optional<DocumentChunk> findByIdWithDocument(UUID id);
}
