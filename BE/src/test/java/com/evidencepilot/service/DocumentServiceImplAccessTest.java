package com.evidencepilot.service;

import com.evidencepilot.mapper.DocumentMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.impl.DocumentPersistenceService;
import com.evidencepilot.service.impl.DocumentServiceImpl;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplAccessTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentTextRepository documentTextRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private ProjectDocumentRepository projectDocumentRepository;

    @Mock
    private PaperSectionRepository paperSectionRepository;

    @Mock
    private ClaimEvidenceMappingRepository claimEvidenceMappingRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private DocumentPersistenceService documentPersistenceService;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private MinioClient minioClient;

    @Test
    void getDocumentByIdRequiresProjectAccess() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.ARCHIVED);
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        service().getDocumentById(document.getId());

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void getDocumentChunksRequiresProjectAccess() {
        User user = user();
        Project project = project();
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId()))
                .thenReturn(List.of());

        service().getDocumentChunks(document.getId());

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void deleteDocumentRequiresProjectWriteAccess() {
        User user = user();
        Project project = project();
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        service().deleteDocument(document.getId());

        verify(currentUserService).requireProjectWriteAccess(user, project);
    }

    @Test
    void deleteDocumentBlocksWhenSourceHasActiveMappings() {
        User user = user();
        Project project = project();
        Document source = document(project);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(claimEvidenceMappingRepository.findByDocumentChunkDocumentIdAndStatus(
                source.getId(), com.evidencepilot.model.enums.MappingStatus.ACTIVE))
                .thenReturn(List.of(new com.evidencepilot.model.ClaimEvidenceMapping()));

        assertThatThrownBy(() -> service().deleteDocument(source.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("mapped to 1 active claim");
        verify(documentRepository, never()).save(source);
    }

    @Test
    void uploadDocumentRequiresProjectWriteAccess() throws Exception {
        User user = user();
        Project project = project();
        Document persisted = document(project);
        persisted.setId(UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentPersistenceService.savePendingDocument(
                eq(project), any(), eq(user), eq(DocumentType.PAPER),
                eq("paper.pdf"), eq("application/pdf"), eq(7L)))
                .thenReturn(persisted);
        when(documentPersistenceService.markDocumentAsUploaded(
                eq(persisted.getId()), anyString()))
                .thenReturn(persisted);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        service().uploadDocument(project.getId(), file, DocumentType.PAPER);

        verify(currentUserService).requireProjectWriteAccess(user, project);
    }

    @Test
    void uploadDocumentActivatesDraftProjectWhenPaperAndSourcePresent() throws Exception {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.ASSIGNED);
        Document persisted = document(project);
        persisted.setId(UUID.randomUUID());
        Document sourceDoc = document(project);
        sourceDoc.setDocType(DocumentType.SOURCE);
        sourceDoc.setId(UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentPersistenceService.savePendingDocument(
                eq(project), any(), eq(user), eq(DocumentType.PAPER),
                eq("paper.pdf"), eq("application/pdf"), eq(7L)))
                .thenReturn(persisted);
        when(documentPersistenceService.markDocumentAsUploaded(
                eq(persisted.getId()), anyString()))
                .thenReturn(persisted);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                eq(project.getId()), eq(DocumentType.PAPER)))
                .thenReturn(List.of(persisted));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                eq(project.getId()), eq(DocumentType.SOURCE)))
                .thenReturn(List.of(sourceDoc));

        service().uploadDocument(project.getId(), file, DocumentType.PAPER);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        verify(projectRepository).save(project);
    }

    @Test
    void deleteDocumentDowngradesActiveProjectWhenRequiredTypeMissing() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.IN_PROGRESS);
        Document source = document(project);
        source.setDocType(DocumentType.SOURCE);
        Document paper = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.PAPER))
                .thenReturn(List.of(paper));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.SOURCE))
                .thenReturn(List.of());

        service().deleteDocument(source.getId());

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ASSIGNED);
        verify(projectRepository).save(project);
    }

    @Test
    void uploadDocumentRejectsCompletedProject() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.APPROVED);
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(readOnly()).when(currentUserService).requireProjectWriteAccess(user, project);

        assertThatThrownBy(() -> service().uploadDocument(project.getId(), file, DocumentType.PAPER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void deleteDocumentRejectsArchivedProject() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.ARCHIVED);
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        doThrow(readOnly()).when(currentUserService).requireProjectWriteAccess(user, project);

        assertThatThrownBy(() -> service().deleteDocument(document.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void archivedProjectRejectsShareAndRemoveSharedDocument() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.ARCHIVED);
        com.evidencepilot.model.Collection collection = collection();
        Document source = document(null);
        source.setDocType(DocumentType.SOURCE);
        source.setCollection(collection);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(documentRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(readOnly()).when(currentUserService).requireProjectWriteAccess(user, project);

        assertThatThrownBy(() -> service().shareToProject(collection.getId(), source.getId(), project.getId()))
                .hasMessageContaining("Project is read-only.");
        assertThatThrownBy(() -> service().removeSharedDocument(project.getId(), source.getId()))
                .hasMessageContaining("Project is read-only.");
        verify(projectDocumentRepository, never()).save(any());
        verify(projectDocumentRepository, never()).delete(any());
    }

    @Test
    void archivedProjectRejectsExtractionAffectingFileAttachment() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.ARCHIVED);
        Document document = document(project);
        document.setProcessingStatus(ProcessingStatus.METADATA_FETCHED);
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        doThrow(readOnly()).when(currentUserService).requireProjectWriteAccess(user, project);

        assertThatThrownBy(() -> service().attachFileToDocument(document.getId(), file))
                .hasMessageContaining("Project is read-only.");
        verify(documentPersistenceService, never()).markDocumentAsUploaded(any(), anyString());
    }

    @Test
    void submittedProjectLocksLinkedDocumentMutationForNonAdmin() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.SUBMITTED_FOR_REVIEW);
        com.evidencepilot.model.Collection collection = collection();
        Document source = document(null);
        source.setCollection(collection);
        com.evidencepilot.model.ProjectDocument link = new com.evidencepilot.model.ProjectDocument();
        link.setProject(project);
        link.setDocument(source);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(projectDocumentRepository.findByDocumentId(source.getId())).thenReturn(List.of(link));

        assertThatThrownBy(() -> service().deleteDocument(source.getId()))
                .hasMessageContaining("Project is locked and cannot be modified.");
        verify(documentRepository, never()).save(source);
    }

    @Test
    void submittedProjectAllowsLinkedDocumentMutationForAdmin() {
        User admin = user();
        Project project = project();
        project.setStatus(ProjectStatus.SUBMITTED_FOR_REVIEW);
        Document source = document(null);
        com.evidencepilot.model.ProjectDocument link = new com.evidencepilot.model.ProjectDocument();
        link.setProject(project);
        link.setDocument(source);

        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(documentRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(projectDocumentRepository.findByDocumentId(source.getId())).thenReturn(List.of(link));

        service().deleteDocument(source.getId());

        verify(documentRepository).save(source);
    }

    @Test
    void getSourceByIdRequiresAccessAndSourceType() {
        User user = user();
        Project project = project();
        Document source = document(project);
        source.setDocType(DocumentType.SOURCE);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(source.getId())).thenReturn(Optional.of(source));

        assertThat(service().getSourceById(source.getId()).id()).isEqualTo(source.getId());

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void getAllPapersForCurrentUserFiltersInactiveAndSourceDocuments() {
        User admin = user();
        Document paper = document(null);
        Document source = document(null);
        source.setDocType(DocumentType.SOURCE);
        Document inactive = document(null);
        inactive.setActive(false);
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(documentRepository.findAll()).thenReturn(List.of(paper, source, inactive));

        assertThat(service().getAllPapersForCurrentUser()).singleElement()
                .extracting("id").isEqualTo(paper.getId());
    }

    @Test
    void projectDocumentQueriesRequireAccessAndReturnPagedResults() {
        User user = user();
        Project project = project();
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service().getDocumentsByProject(project.getId())).isEmpty();
        assertThat(service().getDocumentsByProject(
                project.getId(), 0, 20, "createdAt,desc", null, null, null, true).content()).isEmpty();
        assertThat(service().getSourcesByProject(
                project.getId(), 0, 20, "createdAt,desc", null, ProcessingStatus.READY, true).content()).isEmpty();
        verify(currentUserService, times(3)).requireProjectAccess(user, project);
    }

    @Test
    void sourcePagingMergesDirectAndSharedDocumentsBeforePaging() {
        User user = user();
        Project project = project();
        Document direct = document(project);
        direct.setDocType(DocumentType.SOURCE);
        direct.setOriginalFilename("b.pdf");
        Document shared = document(null);
        shared.setDocType(DocumentType.SOURCE);
        shared.setOriginalFilename("a.pdf");
        ProjectDocument projectDocument = new ProjectDocument();
        projectDocument.setProject(project);
        projectDocument.setDocument(shared);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.findAll(any(Specification.class))).thenReturn(List.of(direct));
        when(projectDocumentRepository.findByProjectId(project.getId()))
                .thenReturn(List.of(projectDocument));

        var first = service().getSourcesByProject(
                project.getId(), 0, 1, "originalFilename,asc", null, null, true);
        var second = service().getSourcesByProject(
                project.getId(), 1, 1, "originalFilename,asc", null, null, true);

        assertThat(first.content()).singleElement()
                .extracting(response -> response.originalFilename())
                .isEqualTo("a.pdf");
        assertThat(first.totalElements()).isEqualTo(2);
        assertThat(first.last()).isFalse();
        assertThat(second.content()).singleElement()
                .extracting(response -> response.originalFilename())
                .isEqualTo("b.pdf");
        assertThat(second.last()).isTrue();
    }

    @Test
    void getDocumentTextMapsExistingTextAndRejectsMissingText() {
        User user = user();
        Project project = project();
        Document document = document(project);
        DocumentText text = new DocumentText();
        text.setId(UUID.randomUUID());
        text.setDocument(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentTextRepository.findByDocumentId(document.getId())).thenReturn(text, null);

        service().getDocumentText(document.getId());
        verify(documentMapper).toDocumentTextResponse(text);

        assertThatThrownBy(() -> service().getDocumentText(document.getId()))
                .hasMessageContaining("Document text not found");
    }

    private DocumentServiceImpl service() {
        var service = new DocumentServiceImpl(
                documentRepository,
                documentChunkRepository,
                documentTextRepository,
                projectRepository,
                collectionRepository,
                projectDocumentRepository,
                paperSectionRepository,
                claimEvidenceMappingRepository,
                currentUserService,
                documentPersistenceService,
                documentMapper,
                minioClient);
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
        return service;
    }

    private ResponseStatusException readOnly() {
        return new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Project is read-only.");
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(user.getId() + "@example.com");
        return user;
    }

    private Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        return project;
    }

    private com.evidencepilot.model.Collection collection() {
        com.evidencepilot.model.Collection collection = new com.evidencepilot.model.Collection();
        collection.setId(UUID.randomUUID());
        collection.setActive(true);
        return collection;
    }

    private Document document(Project project) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);
        document.setUploadedBy(user());
        document.setDocType(DocumentType.PAPER);
        document.setFileUrl("file");
        document.setActive(true);
        return document;
    }
}
