package com.evidencepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.*;
import com.evidencepilot.service.impl.TraceabilityExportServiceImpl;
import com.evidencepilot.service.GapDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TraceabilityExportServiceImplTest {

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ClaimRepository claims = mock(ClaimRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentReferenceRepository references = mock(DocumentReferenceRepository.class);
    private final FeedbackRequestRepository feedback = mock(FeedbackRequestRepository.class);
    private final DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
    private final AiSuggestionRepository aiSuggestions = mock(AiSuggestionRepository.class);
    private final ClaimEvidenceMappingRepository claimEvMappings = mock(ClaimEvidenceMappingRepository.class);
    private final CurrentUserService currentUsers = mock(CurrentUserService.class);
    private final GapDetectionService gapDetection = mock(GapDetectionService.class);
    private TraceabilityExportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceabilityExportServiceImpl(
                projects, claims, documents, references, feedback, chunks,
                aiSuggestions, claimEvMappings, currentUsers, gapDetection, new ObjectMapper());
    }

    @Test
    void exportTraceability_returnsEmptyExportAndMissingTitleMarker() {
        UUID projectId = UUID.randomUUID();
        User user = new User();
        Project project = project(projectId, true);
        project.setTitle(" ");
        when(currentUsers.requireCurrentUser()).thenReturn(user);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(references.findByDocumentProjectIdAndDocumentDocTypeAndDocumentActiveTrueOrderByDocumentIdAscReferenceIndexAsc(
                projectId, DocumentType.SOURCE)).thenReturn(List.of());
        when(claims.findByProjectId(projectId)).thenReturn(List.of());
        when(documents.findByProjectIdAndDocTypeAndActiveTrue(projectId, DocumentType.SOURCE)).thenReturn(List.of());
        when(feedback.findByProjectId(projectId)).thenReturn(List.of());

        var response = service.exportTraceability(projectId);

        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.projectTitle()).isEqualTo("MISSING");
        assertThat(response.claims()).isEmpty();
        verify(currentUsers).requireProjectAccess(user, project);
    }

    @Test
    void exportTraceability_rejectsMissingOrInactiveProject() {
        UUID id = UUID.randomUUID();
        when(currentUsers.requireCurrentUser()).thenReturn(new User());
        assertThatThrownBy(() -> service.exportTraceability(id)).hasMessageContaining(id.toString());

        when(projects.findById(id)).thenReturn(Optional.of(project(id, false)));
        assertThatThrownBy(() -> service.exportTraceability(id)).hasMessageContaining(id.toString());
    }

    private static Project project(UUID id, boolean active) {
        Project project = new Project();
        project.setId(id);
        project.setActive(active);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setTitle("Project");
        return project;
    }
}
