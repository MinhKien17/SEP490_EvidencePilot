package com.evidencepilot.repository;

import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.model.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {
    List<ProjectDocument> findByProjectId(UUID projectId);
    Optional<ProjectDocument> findByProjectIdAndDocumentId(UUID projectId, UUID documentId);
    boolean existsByProjectIdAndDocumentId(UUID projectId, UUID documentId);
    boolean existsByDocumentId(UUID documentId);
    List<ProjectDocument> findByDocumentId(UUID documentId);
    List<ProjectDocument> findByProjectCollectionId(UUID projectCollectionId);
    boolean existsByProjectIdAndDocument_DocTypeAndDocument_ActiveTrue(UUID projectId, DocumentType docType);
}
