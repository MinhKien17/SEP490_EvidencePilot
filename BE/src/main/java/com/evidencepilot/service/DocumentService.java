package com.evidencepilot.service;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    DocumentResponse getDocumentById(UUID id);
    DocumentResponse getSourceById(UUID id);
    List<DocumentResponse> getDocumentsByProject(UUID projectId);
    List<DocumentResponse> getAllPapersForCurrentUser();
    PagedResponse<DocumentResponse> getDocumentsByProject(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            DocumentType docType,
            ProcessingStatus processingStatus,
            Boolean active);
    PagedResponse<DocumentResponse> getSourcesByProject(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            ProcessingStatus processingStatus,
            Boolean active);
    DocumentResponse uploadDocument(UUID projectId, MultipartFile file, DocumentType docType);

    DocumentResponse uploadDocument(UUID projectId, UUID collectionId, MultipartFile file, DocumentType docType);

    List<DocumentChunkResponse> getDocumentChunks(UUID documentId);
    DocumentTextResponse getDocumentText(UUID documentId);
    void deleteDocument(UUID id);
}
