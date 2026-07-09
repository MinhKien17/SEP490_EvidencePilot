package com.evidencepilot.service;

import com.evidencepilot.mapper.DocumentMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.service.impl.DocumentServiceImpl;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private SourceCategoryRepository sourceCategoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SourceExtractionService sourceExtractionService;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private MinioClient minioClient;

    @Test
    void getDocumentByIdRequiresProjectAccess() {
        User user = user();
        Project project = project();
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
    void deleteDocumentRequiresProjectAccess() {
        User user = user();
        Project project = project();
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        service().deleteDocument(document.getId());

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void uploadDocumentRequiresProjectAccess() throws Exception {
        User user = user();
        Project project = project();
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(UUID.randomUUID());
            return document;
        });
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        service().uploadDocument(project.getId(), file, DocumentType.PAPER);

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void uploadDocumentActivatesDraftProjectWhenPaperAndSourcePresent() throws Exception {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.DRAFT);
        Document paper = document(project);
        Document source = document(project);
        source.setDocType(DocumentType.SOURCE);
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(UUID.randomUUID());
            return document;
        });
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.PAPER))
                .thenReturn(List.of(paper));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.SOURCE))
                .thenReturn(List.of(source));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        service().uploadDocument(project.getId(), file, DocumentType.PAPER);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository).save(project);
    }

    @Test
    void deleteDocumentDowngradesActiveProjectWhenRequiredTypeMissing() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.ACTIVE);
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

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DRAFT);
        verify(projectRepository).save(project);
    }

    @Test
    void uploadDocumentRejectsCompletedProject() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.COMPLETED);
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

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

        assertThatThrownBy(() -> service().deleteDocument(document.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadSourceWithCategoryPersistsCategory() throws Exception {
        User user = user();
        com.evidencepilot.model.Collection collection = collection();
        SourceCategory category = category(true);
        MockMultipartFile file = new MockMultipartFile(
                "file", "source.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(sourceCategoryRepository.findByIdAndActiveTrue(category.getId())).thenReturn(Optional.of(category));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(UUID.randomUUID());
            return document;
        });
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        var response = service().uploadDocument(
                null, collection.getId(), category.getId(), file, DocumentType.SOURCE);

        assertThat(response.sourceCategoryId()).isEqualTo(category.getId());
        assertThat(response.sourceCategoryName()).isEqualTo(category.getName());
    }

    @Test
    void uploadSourceWithoutCategoryStoresNull() throws Exception {
        User user = user();
        com.evidencepilot.model.Collection collection = collection();
        MockMultipartFile file = new MockMultipartFile(
                "file", "source.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(UUID.randomUUID());
            return document;
        });
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        var response = service().uploadDocument(
                null, collection.getId(), null, file, DocumentType.SOURCE);

        assertThat(response.sourceCategoryId()).isNull();
        assertThat(response.sourceCategoryName()).isNull();
    }

    @Test
    void uploadSourceRejectsInactiveCategory() {
        User user = user();
        com.evidencepilot.model.Collection collection = collection();
        SourceCategory category = category(false);
        MockMultipartFile file = new MockMultipartFile(
                "file", "source.pdf", "application/pdf", "content".getBytes());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(sourceCategoryRepository.findByIdAndActiveTrue(category.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().uploadDocument(
                null, collection.getId(), category.getId(), file, DocumentType.SOURCE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Source category not found");
    }

    @Test
    void getSourcesByCollectionChecksCollectionAccessAndFiltersCategory() {
        User user = user();
        com.evidencepilot.model.Collection collection = collection();
        SourceCategory category = category(true);
        Document matched = document(null);
        matched.setDocType(DocumentType.SOURCE);
        matched.setCollection(collection);
        matched.setSourceCategory(category);
        Document otherCategory = document(null);
        otherCategory.setDocType(DocumentType.SOURCE);
        otherCategory.setCollection(collection);
        otherCategory.setSourceCategory(category(true));

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(documentRepository.findByCollectionId(collection.getId())).thenReturn(List.of(matched, otherCategory));

        var results = service().getSourcesByCollection(collection.getId(), category.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(matched.getId());
        verify(currentUserService).requireCollectionAccess(user, collection);
    }

    private DocumentServiceImpl service() {
        return new DocumentServiceImpl(
                documentRepository,
                documentChunkRepository,
                documentTextRepository,
                projectRepository,
                collectionRepository,
                sourceCategoryRepository,
                currentUserService,
                sourceExtractionService,
                documentMapper,
                minioClient);
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

    private SourceCategory category(boolean active) {
        SourceCategory category = new SourceCategory();
        category.setId(UUID.randomUUID());
        category.setName(active ? "Journal" : "Inactive");
        category.setActive(active);
        return category;
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
