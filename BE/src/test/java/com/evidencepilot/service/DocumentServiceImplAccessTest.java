package com.evidencepilot.service;

import com.evidencepilot.mapper.DocumentMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

        verify(currentUserService).requireProjectAccess(user, project);
    }

    private DocumentServiceImpl service() {
        return new DocumentServiceImpl(
                documentRepository,
                documentChunkRepository,
                documentTextRepository,
                projectRepository,
                collectionRepository,
                currentUserService,
                documentPersistenceService,
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
