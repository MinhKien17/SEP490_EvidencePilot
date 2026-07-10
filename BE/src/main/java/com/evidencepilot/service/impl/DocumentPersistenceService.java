package com.evidencepilot.service.impl;

import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.event.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentPersistenceService {

    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Document savePendingDocument(
            com.evidencepilot.model.Project project,
            com.evidencepilot.model.Collection collection,
            User uploadedBy,
            DocumentType docType,
            String originalFilename,
            String contentType,
            long fileSizeBytes) {
        Document document = new Document();
        document.setProject(project);
        document.setCollection(collection);
        document.setUploadedBy(uploadedBy);
        document.setDocType(docType);
        document.setFileUrl("pending");
        document.setOriginalFilename(originalFilename);
        document.setContentType(contentType);
        document.setFileSizeBytes(fileSizeBytes);
        document.setProcessingStatus(ProcessingStatus.PENDING_UPLOAD);
        document.setActive(true);
        document.setCreatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    @Transactional
    public Document markDocumentAsUploaded(UUID documentId, String fileUrl) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
        document.setProcessingStatus(ProcessingStatus.UPLOADED);
        document.setFileUrl(fileUrl);
        Document saved = documentRepository.save(document);
        eventPublisher.publishEvent(new DocumentUploadedEvent(saved.getId()));
        return saved;
    }
}
