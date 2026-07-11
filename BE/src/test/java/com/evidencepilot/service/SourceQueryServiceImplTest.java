package com.evidencepilot.service;

import com.evidencepilot.model.*;
import com.evidencepilot.repository.*;
import com.evidencepilot.service.impl.SourceQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SourceQueryServiceImplTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentTextRepository texts = mock(DocumentTextRepository.class);
    private final DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final CurrentUserService currentUsers = mock(CurrentUserService.class);
    private SourceQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SourceQueryServiceImpl(documents, texts, chunks, projects, currentUsers);
    }

    @Test
    void getDocumentTextsByProject_filtersDocumentsWithoutText() {
        UUID projectId = UUID.randomUUID();
        User user = new User();
        Project project = new Project();
        Document withText = document(project);
        Document withoutText = document(project);
        DocumentText text = new DocumentText();
        text.setId(UUID.randomUUID());
        text.setDocument(withText);
        text.setExtractedText("evidence");
        text.setExtractionMethod("raw");
        when(currentUsers.requireCurrentUser()).thenReturn(user);
        when(currentUsers.isAdmin(user)).thenReturn(false);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(documents.findByProjectId(projectId)).thenReturn(List.of(withText, withoutText));
        when(texts.findByDocumentId(withText.getId())).thenReturn(text);

        assertThat(service.getDocumentTextsByProject(projectId))
                .singleElement().extracting("extractedText").isEqualTo("evidence");
        verify(currentUsers).requireProjectAccess(user, project);
    }

    @Test
    void getProjectDocumentText_checksProjectMembershipAndReturnsText() {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        User user = new User();
        Project project = new Project();
        project.setId(projectId);
        Document document = document(project);
        document.setId(documentId);
        DocumentText text = new DocumentText();
        text.setId(UUID.randomUUID());
        text.setDocument(document);
        text.setExtractedText("full text");
        text.setExtractionMethod("raw");
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(documents.findById(documentId)).thenReturn(Optional.of(document));
        when(texts.findByDocumentId(documentId)).thenReturn(text);

        assertThat(service.getProjectDocumentText(projectId, documentId, user).extractedText())
                .isEqualTo("full text");
        verify(currentUsers).requireProjectAccess(user, project);
    }

    @Test
    void getProjectDocumentText_rejectsDocumentFromAnotherProject() {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        Project other = new Project();
        other.setId(UUID.randomUUID());
        Document document = document(other);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(documents.findById(documentId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.getProjectDocumentText(projectId, documentId, new User()))
                .hasMessageContaining("not found in this project");
    }

    @Test
    void getDocumentChunks_mapsChunkFields() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(document);
        chunk.setChunkIndex(2);
        chunk.setText("chunk text");
        chunk.setActive(true);
        when(chunks.findByDocumentId(documentId)).thenReturn(List.of(chunk));

        assertThat(service.getDocumentChunks(documentId))
                .singleElement().satisfies(response -> {
                    assertThat(response.chunkIndex()).isEqualTo(2);
                    assertThat(response.text()).isEqualTo("chunk text");
                });
    }

    private static Document document(Project project) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);
        return document;
    }
}
