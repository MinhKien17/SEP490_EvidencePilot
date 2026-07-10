package com.evidencepilot.dto.response;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    UUID projectId,
    UUID collectionId,
    UUID sourceCategoryId,
    String sourceCategoryName,
    UUID uploadedBy,
    DocumentType docType,
    String fileUrl,
    String originalFilename,
    String contentType,
    Long fileSizeBytes,
    String fileHashSha256,
    ProcessingStatus processingStatus,
    boolean active,
    LocalDateTime createdAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
            doc.getId(),
            doc.getProject() != null ? doc.getProject().getId() : null,
            doc.getCollection() != null ? doc.getCollection().getId() : null,
            doc.getSourceCategory() != null ? doc.getSourceCategory().getId() : null,
            doc.getSourceCategory() != null ? doc.getSourceCategory().getName() : null,
            doc.getUploadedBy().getId(),
            doc.getDocType(),
            doc.getFileUrl(),
            doc.getOriginalFilename(),
            doc.getContentType(),
            doc.getFileSizeBytes(),
            doc.getFileHashSha256(),
            doc.getProcessingStatus(),
            doc.isActive(),
            doc.getCreatedAt()
        );
    }
}
