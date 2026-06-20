package com.evidencepilot.service;

import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.dto.response.ProjectResponse;

import java.util.List;

/**
 * Business operations for the {@code Project} entity.
 *
 * <p>
 * <strong>Tenancy contract</strong>: every method requires the
 * {@code authenticatedStudentId} parameter. This ID is extracted from the
 * Spring Security context by the controller — it must never originate
 * from a client-supplied value.
 * </p>
 */
public interface ProjectService {

    /**
     * Lists all active projects belonging to the authenticated student.
     */
    List<ProjectResponse> getAllProjects(Integer authenticatedStudentId);

    /**
     * Retrieves a single active project by ID, scoped to the authenticated student.
     *
     * @throws com.evidencepilot.exception.ResourceNotFoundException
     *         if the project does not exist or does not belong to the student
     */
    ProjectResponse getProjectById(Integer id, Integer authenticatedStudentId);

    /**
     * Creates a new project owned by the authenticated student.
     * Status defaults to {@code DRAFT}; active defaults to {@code true}.
     */
    ProjectResponse createProject(ProjectCreateRequest request, Integer authenticatedStudentId);

    /**
     * Updates the title and description of an existing project
     * owned by the authenticated student.
     *
     * @throws com.evidencepilot.exception.ResourceNotFoundException
     *         if the project does not exist or does not belong to the student
     */
    ProjectResponse updateProject(Integer id, ProjectUpdateRequest request, Integer authenticatedStudentId);

    /**
     * Soft-deletes a project by setting {@code active = false} and
     * {@code status = DELETED}.
     *
     * @throws com.evidencepilot.exception.ResourceNotFoundException
     *         if the project does not exist or does not belong to the student
     */
    void deleteProject(Integer id, Integer authenticatedStudentId);
}
