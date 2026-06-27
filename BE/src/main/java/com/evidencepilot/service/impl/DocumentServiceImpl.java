package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.DocumentMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.SourceExtractionService;
import com.evidencepilot.support.PagingRequest;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String BUCKET = "evidence-pilot-bucket";
    private static final Set<String> DOCUMENT_SORT_FIELDS = Set.of(
            "originalFilename", "docType", "processingStatus", "createdAt", "fileSizeBytes");

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentTextRepository documentTextRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final SourceExtractionService sourceExtractionService;
    private final DocumentMapper documentMapper;
    private final MinioClient minioClient;

    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(BUCKET).build());
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(BUCKET).build());
                log.info("Created MinIO bucket: {}", BUCKET);
            }
        } catch (Exception e) {
            log.warn("Could not verify/create MinIO bucket '{}': {}", BUCKET, e.getMessage());
        }
    }

    @Override
    public DocumentResponse getDocumentById(UUID id) {
        return DocumentResponse.from(documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Document")));
    }

    @Override
    public List<DocumentResponse> getDocumentsByProject(UUID projectId) {
        return documentRepository.findByProjectId(projectId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Override
    public PagedResponse<DocumentResponse> getDocumentsByProject(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            DocumentType docType,
            ProcessingStatus processingStatus,
            Boolean active) {
        requireProjectAccess(projectId);
        var pageable = PagingRequest.pageable(
                page, size, sort, DOCUMENT_SORT_FIELDS, "createdAt,desc");
        var results = documentRepository.findAll(
                documentSpec(projectId, docType, processingStatus, active, q),
                pageable);
        return PagedResponse.from(results.map(DocumentResponse::from));
    }

    @Override
    public PagedResponse<DocumentResponse> getSourcesByProject(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            ProcessingStatus processingStatus,
            Boolean active) {
        return getDocumentsByProject(
                projectId,
                page,
                size,
                sort,
                q,
                DocumentType.EVIDENCE_SOURCE,
                processingStatus,
                active);
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(UUID projectId, MultipartFile file, DocumentType docType) {
        var currentUser = currentUserService.requireCurrentUser();

        Project project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        }

        String originalName = file.getOriginalFilename();

        // 1. Save Document first — Hibernate auto-generates UUID
        Document document = new Document();
        document.setProject(project);
        document.setUploadedBy(currentUser);
        document.setDocType(docType);
        document.setFileUrl("pending");
        document.setOriginalFilename(originalName);
        document.setContentType(file.getContentType());
        document.setFileSizeBytes(file.getSize());
        document.setProcessingStatus(ProcessingStatus.UPLOADED);
        document.setActive(true);
        document.setCreatedAt(LocalDateTime.now());
        document = documentRepository.save(document);

        // 2. Upload to MinIO using the auto-generated UUID
        String objectKey = "sources/raw/" + document.getId().toString() + ".pdf";
        document.setFileUrl(objectKey);

        try (var in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }

        // 3. Publish document ID to extraction queue
        sourceExtractionService.triggerExtraction(document.getId());

        return DocumentResponse.from(document);
    }

    @Override
    public List<DocumentChunkResponse> getDocumentChunks(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException(documentId, "Document");
        }
        return documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(documentMapper::toDocumentChunkResponse)
                .toList();
    }

    @Override
    public DocumentTextResponse getDocumentText(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException(documentId, "Document");
        }
        var text = documentTextRepository.findByDocumentId(documentId);
        if (text == null) {
            throw new ResourceNotFoundException("Document text not found for document " + documentId);
        }
        return documentMapper.toDocumentTextResponse(text);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Document"));
        doc.setActive(false);
        documentRepository.save(doc);
    }

    private void requireProjectAccess(UUID projectId) {
        var currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);
    }

    private Specification<Document> documentSpec(
            UUID projectId,
            DocumentType docType,
            ProcessingStatus processingStatus,
            Boolean active,
            String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));
            predicates.add(cb.equal(root.get("active"), active != null ? active : true));

            if (docType != null) {
                predicates.add(cb.equal(root.get("docType"), docType));
            }

            if (processingStatus != null) {
                predicates.add(cb.equal(root.get("processingStatus"), processingStatus));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("originalFilename")), like),
                        cb.like(cb.lower(root.get("contentType")), like),
                        cb.like(cb.lower(root.get("fileUrl")), like)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
