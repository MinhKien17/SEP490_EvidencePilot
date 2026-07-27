package com.evidencepilot.repository;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    long countByActiveTrueAndDocType(DocumentType docType);
    long countByProcessingStatus(ProcessingStatus processingStatus);
    Optional<Document> findByFileHashSha256(String fileHash);
    List<Document> findByProjectId(UUID projectId);
    List<Document> findByProjectIdAndDocTypeAndActiveTrue(UUID projectId, DocumentType docType);
    List<Document> findByCollectionId(UUID collectionId);
    List<Document> findByUploadedById(UUID uploadedById);
    List<Document> findByProjectIdOrCollectionId(UUID projectId, UUID collectionId);
    List<Document> findByProcessingStatusAndActiveTrue(ProcessingStatus processingStatus);

    @Modifying
    @Query("UPDATE Document d SET d.processingStatus = :status, d.chunkCount = :chunkCount WHERE d.id = :documentId")
    int updateDocumentStatusAndChunks(
        @Param("documentId") UUID documentId,
        @Param("status") ProcessingStatus status,
        @Param("chunkCount") Integer chunkCount
    );
}
