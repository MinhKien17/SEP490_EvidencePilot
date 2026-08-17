package com.evidencepilot.repository;

import com.evidencepilot.model.CollectionDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionDocumentRepository extends JpaRepository<CollectionDocument, UUID> {
    boolean existsByCollectionIdAndDocumentId(UUID collectionId, UUID documentId);

    Optional<CollectionDocument> findByCollectionIdAndDocumentId(UUID collectionId, UUID documentId);

    @EntityGraph(attributePaths = {"collection", "document"})
    List<CollectionDocument> findByCollectionId(UUID collectionId);

    @EntityGraph(attributePaths = {"collection", "document"})
    List<CollectionDocument> findByDocumentId(UUID documentId);
}
