package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.dto.response.ClaimConsistencyResponse;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.service.ClaimService;
import com.evidencepilot.service.ClaimContentConsistencyService;
import com.evidencepilot.service.CollectionService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.PaperProcessingService;
import com.evidencepilot.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ProjectControllerTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final ClaimService claimService = mock(ClaimService.class);
    private final CollectionService collectionService = mock(CollectionService.class);
    private final PaperProcessingService paperProcessingService = mock(PaperProcessingService.class);
    private final ClaimContentConsistencyService claimContentConsistencyService =
            mock(ClaimContentConsistencyService.class);
    private ProjectController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new ProjectController(
                projectService, documentService, claimService, collectionService,
                paperProcessingService, claimContentConsistencyService);
        when(claimContentConsistencyService.preflight(any()))
                .thenReturn(new ClaimConsistencyResponse(0, List.of()));
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void getAllProjects_bindsPagingAndFilters() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .param("page", "1").param("size", "5")
                        .param("sort", "title,asc").param("q", "audit")
                        .param("status", "IN_PROGRESS").param("active", "true"))
                .andExpect(status().isOk());
        verify(projectService).getAllProjects(1, 5, "title,asc", "audit", ProjectStatus.IN_PROGRESS, true);
    }

    @Test
    void getProjectById_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{id}", id)).andExpect(status().isOk());
        verify(projectService).getProjectById(id);
    }

    @Test
    void createProject_returns201() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Audit\",\"description\":\"Evidence\",\"targetStandard\":\"CUSTOM\"}"))
                .andExpect(status().isCreated());
        verify(projectService).createProject(any(ProjectCreateRequest.class));
    }

    @Test
    void updateProject_bindsIdAndBody() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/api/projects/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
        verify(projectService).updateProject(eq(id), any(ProjectUpdateRequest.class));
    }

    @Test
    void completeProject_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/projects/{id}/complete", id)).andExpect(status().isOk());
        verify(projectService).completeProject(id);
    }

    @Test
    void archiveProject_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/projects/{id}/archive", id)).andExpect(status().isOk());
        verify(projectService).archiveProject(id);
    }

    @Test
    void unarchiveProject_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/projects/{id}/unarchive", id)).andExpect(status().isOk());
        verify(projectService).unarchiveProject(id);
    }

    @Test
    void deleteProject_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/projects/{id}", id)).andExpect(status().isNoContent());
        verify(projectService).deleteProject(id);
    }

    @Test
    void getProjectMembers_usesDtoServiceMethod() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{id}/members", id)).andExpect(status().isOk());
        verify(projectService).getProjectMemberResponses(id);
    }

    @Test
    void getProjectDocuments_bindsDefaultPaging() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{id}/documents", id)).andExpect(status().isOk());
        verify(documentService).getDocumentsByProject(id, 0, 20, "createdAt,desc", null, null, null, null);
    }

    @Test
    void getProjectSources_bindsDefaultPaging() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{id}/sources", id)).andExpect(status().isOk());
        verify(documentService).getSourcesByProject(id, 0, 20, "createdAt,desc", null, null, null);
    }

    @Test
    void getProjectClaims_bindsDefaultPaging() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{id}/claims", id)).andExpect(status().isOk());
        verify(claimService).getClaimsByProject(id, 0, 20, "createdAt,desc", null, null, null);
    }

    @Test
    void getProjectClaims_bindsSectionId() throws Exception {
        UUID id = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{id}/claims", id).param("sectionId", sectionId.toString()))
                .andExpect(status().isOk());
        verify(claimService).getClaimsByProject(id, 0, 20, "createdAt,desc", null, null, sectionId);
    }

    @Test
    void exportProjectStreamsArchiveAndDeletesTemporaryFile() throws Exception {
        UUID projectId = UUID.randomUUID();
        byte[] expected = {1, 2, 3};
        Path archive = Files.write(Files.createTempFile("project-export-", ".zip"), expected);
        when(paperProcessingService.exportTexArchive(projectId)).thenReturn(archive);

        try {
            MvcResult result = mockMvc.perform(get("/api/projects/{projectId}/export", projectId)
                            .param("format", "tex"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"export.zip\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(content().bytes(expected));
            assertThat(archive).doesNotExist();
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    @Test
    void exportProjectDeletesTemporaryFileWhenStreamingFails() throws Exception {
        UUID projectId = UUID.randomUUID();
        Path archive = Files.write(Files.createTempFile("project-export-", ".zip"), new byte[] {1});
        when(paperProcessingService.exportTexArchive(projectId)).thenReturn(archive);
        ResponseEntity<StreamingResponseBody> response = controller.exportProject(projectId, "tex");
        OutputStream failingOutput = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("stream failed");
            }
        };

        try {
            assertThatThrownBy(() -> response.getBody().writeTo(failingOutput))
                    .isInstanceOf(IOException.class)
                    .hasMessage("stream failed");
            assertThat(archive).doesNotExist();
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    @Test
    void addMember_returns201AndBindsRole() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .param("userId", userId.toString()).param("role", "MEMBER"))
                .andExpect(status().isCreated());
        verify(projectService).addMember(projectId, userId, ProjectRole.MEMBER);
    }

    @Test
    void removeMember_returns204() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        mockMvc.perform(delete("/api/projects/{id}/members/{userId}", projectId, userId))
                .andExpect(status().isNoContent());
        verify(projectService).removeMember(projectId, userId);
    }
}
