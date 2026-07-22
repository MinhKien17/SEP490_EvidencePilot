package com.evidencepilot.service;

import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.dto.response.ProjectMemberResponse;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import java.util.List;
import java.util.UUID;

public interface ProjectService {
    List<ProjectResponse> getAllProjects();
    PagedResponse<ProjectResponse> getAllProjects(
            int page,
            int size,
            String sort,
            String q,
            ProjectStatus status,
            Boolean active);
    ProjectResponse getProjectById(UUID id);
    ProjectResponse createProject(ProjectCreateRequest request);
    ProjectResponse updateProject(UUID id, ProjectUpdateRequest request);
    ProjectResponse completeProject(UUID id);
    ProjectResponse archiveProject(UUID id);
    ProjectResponse unarchiveProject(UUID id);
    void deleteProject(UUID id);
    List<ProjectMember> getProjectMembers(UUID projectId);
    List<ProjectMemberResponse> getProjectMemberResponses(UUID projectId);
    void addMember(UUID projectId, UUID userId, ProjectRole role);
    void removeMember(UUID projectId, UUID userId);
}
