package com.evidencepilot.service.impl;

import com.evidencepilot.client.openalex.OpenAlexClient;
import com.evidencepilot.dto.openalex.OpenAlexWorkResponse;
import com.evidencepilot.dto.response.OpenAlexPreview;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentObjectStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAlexIngestionServiceImplTest {

    @Mock
    private OpenAlexClient openAlexClient;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private DocumentObjectStorage documentObjectStorage;
    @Mock
    private DocumentPersistenceService documentPersistenceService;

    private OpenAlexIngestionServiceImpl service;
    private OpenAlexIngestionServiceImpl serviceSpy;
    private User currentUser;
    private Project project;
    private OpenAlexWorkResponse sampleWork;

    @BeforeEach
    void setUp() {
        service = new OpenAlexIngestionServiceImpl(
                openAlexClient, documentRepository, projectRepository,
                currentUserService, documentObjectStorage,
                documentPersistenceService, new ObjectMapper());
        serviceSpy = spy(service);

        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("test@test.com");
        currentUser.setRole(UserRole.STUDENT);

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Test Project");
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setActive(true);

        sampleWork = new OpenAlexWorkResponse(
                "https://openalex.org/W123",
                "https://doi.org/10.1000/xyz123",
                "Test Paper",
                List.of(new OpenAlexWorkResponse.OpenAlexAuthor(
                        new OpenAlexWorkResponse.Author("Alice Smith"))),
                new OpenAlexWorkResponse.OpenAlexPrimaryLocation(
                        new OpenAlexWorkResponse.OpenAlexSource(
                                "Pub", "Org", "journal", "https://example.com"),
                        "https://example.com/paper.pdf",
                        "https://example.com/paper",
                        "cc-by", "acceptedVersion", true),
                null,
                new OpenAlexWorkResponse.OpenAlexOpenAccess(true, "green", "https://example.com/paper.pdf", true),
                null,
                2024
        );
    }

    @Test
    void lookupByDoi_returnsPreview() {
        when(openAlexClient.fetchWork("10.1000/xyz")).thenReturn(sampleWork);

        // lookupByDoi doesn't check urlIsReachable, so use service directly
        OpenAlexPreview preview = service.lookupByDoi("10.1000/xyz");

        assertThat(preview.title()).isEqualTo("Test Paper");
        assertThat(preview.publicationYear()).isEqualTo(2024);
        assertThat(preview.authors()).containsExactly("Alice Smith");
        assertThat(preview.oaUrl()).isEqualTo("https://example.com/paper.pdf");
    }

    @Test
    void ingestByDoi_savesDocumentAndUploadsPdf() {
        String doi = "10.1000/xyz123";
        UUID projectId = project.getId();
        byte[] pdfBytes = "fake-pdf".getBytes();

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(openAlexClient.fetchWork(doi)).thenReturn(sampleWork);
        when(openAlexClient.downloadPdf("https://example.com/paper.pdf"))
                .thenReturn(new ByteArrayInputStream(pdfBytes));
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            var doc = (com.evidencepilot.model.Document) invocation.getArgument(0);
            if (doc.getId() == null) doc.setId(UUID.randomUUID());
            return doc;
        });
        when(documentPersistenceService.markDocumentAsUploaded(any(), anyString()))
                .thenAnswer(invocation -> {
                    var id = (UUID) invocation.getArgument(0);
                    var fileUrl = (String) invocation.getArgument(1);
                    var doc = new com.evidencepilot.model.Document();
                    doc.setId(id);
                    doc.setFileUrl(fileUrl);
                    doc.setProcessingStatus(ProcessingStatus.UPLOADED);
                    doc.setProject(project);
                    doc.setDocType(com.evidencepilot.model.enums.DocumentType.SOURCE);
                    doc.setUploadedBy(currentUser);
                    doc.setActive(true);
                    doc.setOriginalFilename("Test Paper.pdf");
                    doc.setFileUrl(fileUrl);
                    doc.setDoi(doi);
                    doc.setTitle("Test Paper");
                    doc.setPublicationYear(2024);
                    doc.setPublisher("Test Publisher");
                    doc.setCreatedAt(LocalDateTime.now());
                    return doc;
                });

        var result = serviceSpy.ingestByDoi(projectId, doi);

        assertThat(result.processingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
        assertThat(result.originalFilename()).contains("Test Paper");
        verify(documentObjectStorage).write(anyString(), eq(pdfBytes), eq("application/pdf"));
    }

    @Test
    void ingestByDoi_returnsMetadataOnlyWhenOaUrlIsNull() {
        var workNoPdf = new OpenAlexWorkResponse(
                "https://openalex.org/W456", "https://doi.org/10.1000/no-pdf",
                "No PDF", List.of(), null, null, null, null, 2023);

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(openAlexClient.fetchWork("10.1000/no-pdf")).thenReturn(workNoPdf);
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            var doc = (com.evidencepilot.model.Document) invocation.getArgument(0);
            if (doc.getId() == null) doc.setId(UUID.randomUUID());
            return doc;
        });

        var result = service.ingestByDoi(project.getId(), "10.1000/no-pdf");

        assertThat(result.processingStatus()).isEqualTo(ProcessingStatus.METADATA_FETCHED);
        assertThat(result.originalFilename()).contains("No PDF");
    }

    @Test
    void ingestByDoi_throwsWhenProjectNotFound() {
        UUID badId = UUID.randomUUID();
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(projectRepository.findById(badId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ingestByDoi(badId, "10.1000/xyz"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ingestByDoi_throwsWhenProjectIsApproved() {
        project.setStatus(ProjectStatus.APPROVED);
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.ingestByDoi(project.getId(), "10.1000/xyz"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("read-only");
    }
}
