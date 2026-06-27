package com.evidencepilot.repository;

import com.evidencepilot.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    Optional<Document> findByFileHashSha256(String fileHash);
    List<Document> findByProjectId(UUID projectId);
    List<Document> findByCollectionId(UUID collectionId);
    List<Document> findByUploadedById(UUID uploadedById);
    List<Document> findByProjectIdOrCollectionId(UUID projectId, UUID collectionId);
}
