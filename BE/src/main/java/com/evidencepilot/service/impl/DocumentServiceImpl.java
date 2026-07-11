package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.DocumentMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.dto.request.PagingRequest;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
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

    private static final Set<String> DOCUMENT_SORT_FIELDS = Set.of(
            "originalFilename", "docType", "processingStatus", "createdAt", "fileSizeBytes");

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentTextRepository documentTextRepository;
    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository;
    private final SourceCategoryRepository sourceCategoryRepository;
    private final CurrentUserService currentUserService;
    private final DocumentPersistenceService documentPersistenceService;
    private final DocumentMapper documentMapper;
    private final MinioClient minioClient;

    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("Could not verify/create MinIO bucket '{}': {}", bucketName, e.getMessage());
        }
    }

    @Override
    public DocumentResponse getDocumentById(UUID id) {
        var currentUser = currentUserService.requireCurrentUser();
        Document doc = findDocument(id);
        requireDocumentAccess(currentUser, doc);
        return DocumentResponse.from(doc);
    }

    @Override
    public DocumentResponse getSourceById(UUID id) {
        var currentUser = currentUserService.requireCurrentUser();
        Document doc = findDocument(id);
        if (doc.getDocType() != DocumentType.SOURCE || !doc.isActive()) {
            throw new ResourceNotFoundException(id, "Source");
        }
        requireDocumentAccess(currentUser, doc);
        return DocumentResponse.from(doc);
    }

    @Override
    public List<DocumentResponse> getAllPapersForCurrentUser() {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(currentUser)) {
            return documentRepository.findAll().stream()
                    .filter(d -> d.isActive() && d.getDocType() == DocumentType.PAPER)
                    .map(DocumentResponse::from)
                    .toList();
        }
        return documentRepository.findAll().stream()
                .filter(d -> d.isActive() && d.getDocType() == DocumentType.PAPER
                        && d.getProject() != null && d.getProject().getStudent() != null
                        && d.getProject().getStudent().getId().equals(currentUser.getId()))
                .map(DocumentResponse::from)
                .toList();
    }

    @Override
    public List<DocumentResponse> getDocumentsByProject(UUID projectId) {
        requireProjectAccess(projectId);
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
    public List<DocumentResponse> getSourcesByCollection(UUID collectionId, UUID sourceCategoryId) {
        var currentUser = currentUserService.requireCurrentUser();
        var collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException(collectionId, "Collection"));
        currentUserService.requireCollectionAccess(currentUser, collection);
        return documentRepository.findByCollectionId(collectionId).stream()
                .filter(doc -> doc.getDocType() == DocumentType.SOURCE)
                .filter(doc -> sourceCategoryId == null
                        || (doc.getSourceCategory() != null
                                && doc.getSourceCategory().getId().equals(sourceCategoryId)))
                .map(DocumentResponse::from)
                .toList();
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
                DocumentType.SOURCE,
                processingStatus,
                active);
    }

    @Override
    public DocumentResponse uploadDocument(UUID projectId, MultipartFile file, DocumentType docType) {
        return uploadDocument(projectId, null, file, docType);
    }

    @Override
    public DocumentResponse uploadDocument(UUID projectId, UUID collectionId, MultipartFile file, DocumentType docType) {
        return uploadDocument(projectId, collectionId, null, file, docType);
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(
            UUID projectId,
            UUID collectionId,
            UUID sourceCategoryId,
            MultipartFile file,
            DocumentType docType) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        var currentUser = currentUserService.requireCurrentUser();

        Project project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
            currentUserService.requireProjectAccess(currentUser, project);
            requireMutable(project);
        }

        com.evidencepilot.model.Collection collection = null;
        if (collectionId != null) {
            collection = collectionRepository.findById(collectionId)
                    .orElseThrow(() -> new ResourceNotFoundException(collectionId, "Collection"));
            currentUserService.requireCollectionAccess(currentUser, collection);
            if (collection.getProject() != null) {
                requireMutable(collection.getProject());
            }
        }

        var sourceCategory = sourceCategoryId == null ? null
                : sourceCategoryRepository.findByIdAndActiveTrue(sourceCategoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source category not found"));
        if (sourceCategory != null && docType != DocumentType.SOURCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source category applies only to sources");
        }

        String originalName = file.getOriginalFilename();

        // Step A: Save pending document (transactional)
        Document document = documentPersistenceService.savePendingDocument(
                project, collection, currentUser, docType, originalName,
                file.getContentType(), file.getSize());

        // Step B: Upload to MinIO (non-transactional — holds no DB connection)
        String objectKey = "sources/raw/" + document.getId().toString() + fileExtension(originalName);

        try (var in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }

        // Step C: Mark document as uploaded (transactional, publishes event after commit)
        document = documentPersistenceService.markDocumentAsUploaded(document.getId(), objectKey);

        if (project != null) {
            refreshProjectStatus(project);
        }

        return DocumentResponse.from(document);
    }

    @Override
    public List<DocumentChunkResponse> getDocumentChunks(UUID documentId) {
        var currentUser = currentUserService.requireCurrentUser();
        Document doc = findDocument(documentId);
        requireDocumentAccess(currentUser, doc);
        return documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(documentMapper::toDocumentChunkResponse)
                .toList();
    }

    @Override
    public DocumentTextResponse getDocumentText(UUID documentId) {
        var currentUser = currentUserService.requireCurrentUser();
        Document doc = findDocument(documentId);
        requireDocumentAccess(currentUser, doc);
        var text = documentTextRepository.findByDocumentId(documentId);
        if (text == null) {
            throw new ResourceNotFoundException("Document text not found for document " + documentId);
        }
        return documentMapper.toDocumentTextResponse(text);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {
        var currentUser = currentUserService.requireCurrentUser();
        Document doc = findDocument(id);
        requireDocumentAccess(currentUser, doc);
        Project project = doc.getProject();
        if (project == null && doc.getCollection() != null) {
            project = doc.getCollection().getProject();
        }
        if (project != null) {
            requireMutable(project);
        }
        doc.setActive(false);
        documentRepository.save(doc);
        if (doc.getProject() != null) {
            refreshProjectStatus(doc.getProject());
        }
    }

    @Override
    @Transactional
    public void updateDocumentStatusFromWebhook(UUID documentId, String status) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
        doc.setProcessingStatus(ProcessingStatus.valueOf(status));
        documentRepository.save(doc);
        log.info("Webhook updated document {} status to {}", documentId, status);
    }

    private Document findDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Document"));
    }

    private void requireDocumentAccess(User currentUser, Document doc) {
        if (doc.getProject() != null) {
            currentUserService.requireProjectAccess(currentUser, doc.getProject());
            return;
        }
        if (doc.getCollection() != null) {
            currentUserService.requireCollectionAccess(currentUser, doc.getCollection());
            return;
        }
        currentUserService.requireUserIdOrAdmin(currentUser, doc.getUploadedBy().getId());
    }

    private void requireProjectAccess(UUID projectId) {
        var currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);
    }

    private void requireMutable(Project project) {
        if (project.getStatus() == ProjectStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project is read-only.");
        }
    }

    private void refreshProjectStatus(Project project) {
        if (project.getStatus() != ProjectStatus.ASSIGNED && project.getStatus() != ProjectStatus.IN_PROGRESS) {
            return;
        }
        boolean hasPaper = !documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.PAPER)
                .isEmpty();
        boolean hasSource = !documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.SOURCE)
                .isEmpty();
        ProjectStatus status = hasPaper && hasSource ? ProjectStatus.IN_PROGRESS : ProjectStatus.ASSIGNED;
        if (project.getStatus() != status) {
            project.setStatus(status);
            projectRepository.save(project);
        }
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

    private static String fileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return ".bin";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return ".bin";
        }
        String extension = filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!extension.matches("\\.[a-z0-9]{1,12}")) {
            return ".bin";
        }
        return extension;
    }
}
