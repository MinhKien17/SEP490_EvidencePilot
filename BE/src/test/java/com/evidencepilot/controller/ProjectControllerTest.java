package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.service.ClaimService;
import com.evidencepilot.service.CollectionService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ProjectControllerTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final ClaimService claimService = mock(ClaimService.class);
    private final CollectionService collectionService = mock(CollectionService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ProjectController(
                projectService, documentService, claimService, collectionService)).build();
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
                        .content("{\"title\":\"Audit\",\"description\":\"Evidence\",\"targetStandard\":\"ISO\"}"))
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
        verify(claimService).getClaimsByProject(id, 0, 20, "createdAt,desc", null, null);
    }

    @Test
    void getProjectCollections_bindsDefaultPaging() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{id}/collections", id)).andExpect(status().isOk());
        verify(collectionService).getCollectionsByProjectId(id, 0, 20, "createdAt,desc", null, null);
    }

    @Test
    void addMember_returns201AndBindsRole() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .param("userId", userId.toString()).param("role", "EDITOR"))
                .andExpect(status().isCreated());
        verify(projectService).addMember(projectId, userId, ProjectRole.EDITOR);
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
