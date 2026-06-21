package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.dto.response.SourceResponseDto;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.ProjectService;
import com.evidencepilot.service.SourceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Project CRUD operations.
 * Base path: /api/projects
 *
 * <p>
 * <strong>Security</strong>: every endpoint is gated to
 * {@code ROLE_STUDENT} via {@link PreAuthorize}. The authenticated
 * student's ID is extracted server-side from the Spring Security
 * context and forwarded to the service layer — it is never accepted
 * from the client.
 * </p>
 */
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Student project CRUD — all endpoints are tenant-scoped to the authenticated student")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;
    private final SourceQueryService sourceQueryService;

    @Operation(
            summary = "List my projects",
            description = "Returns all active projects owned by the authenticated student. "
                    + "Soft-deleted projects are excluded. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project list returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller does not have STUDENT role")
    })
    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        Integer studentId = getAuthenticatedStudentId();
        return projectService.getAllProjects(studentId);
    }

    @Operation(
            summary = "Get project by ID",
            description = "Returns a single active project by its ID, scoped to the authenticated student. "
                    + "Returns 404 if the project does not exist or belongs to another student. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller does not have STUDENT role"),
            @ApiResponse(responseCode = "404", description = "Project not found or not owned by caller")
    })
    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Integer id) {
        Integer studentId = getAuthenticatedStudentId();
        return projectService.getProjectById(id, studentId);
    }

    @Operation(
            summary = "Create a new project",
            description = "Creates a new project owned by the authenticated student. "
                    + "Status defaults to DRAFT; the student ID is set server-side from the JWT. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — invalid field values"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller does not have STUDENT role")
    })
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request) {
        Integer studentId = getAuthenticatedStudentId();
        ProjectResponse response = projectService.createProject(request, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update project by ID",
            description = "Updates the title and description of an existing project owned by the authenticated student. "
                    + "Returns 404 if the project does not exist or belongs to another student. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — invalid field values"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller does not have STUDENT role"),
            @ApiResponse(responseCode = "404", description = "Project not found or not owned by caller")
    })
    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        Integer studentId = getAuthenticatedStudentId();
        return projectService.updateProject(id, request, studentId);
    }

    @Operation(
            summary = "Soft-delete project by ID",
            description = "Soft-deletes a project by setting active=false and status=DELETED. "
                    + "The record is preserved in the database but excluded from all read queries. "
                    + "Returns 404 if the project does not exist or belongs to another student. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project soft-deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller does not have STUDENT role"),
            @ApiResponse(responseCode = "404", description = "Project not found or not owned by caller")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Integer id) {
        Integer studentId = getAuthenticatedStudentId();
        projectService.deleteProject(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List project sources",
            description = "Returns all active sources associated with the specified project. "
                    + "Enforces tenancy: students can only fetch sources for projects they own; "
                    + "instructors can access projects in review with an active feedback request; "
                    + "admins have unrestricted access. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT, INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sources list returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — project access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/{projectId}/sources")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<SourceResponseDto>> findByProject(@PathVariable Integer projectId) {
        List<SourceResponseDto> sources = sourceQueryService.getSourcesByProject(projectId);
        return ResponseEntity.ok(sources);
    }

    @Operation(
            summary = "Get project source by ID",
            description = "Retrieves a specific source belonging to a project. Enforces project-level read access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Source metadata returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — project access denied"),
            @ApiResponse(responseCode = "404", description = "Source not found or inactive")
    })
    @GetMapping("/{projectId}/sources/{sourceId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<SourceResponseDto> getProjectSource(
            @PathVariable Integer projectId,
            @PathVariable Integer sourceId) {
        User currentUser = currentUserService.requireCurrentUser();
        SourceResponseDto response = sourceQueryService.getProjectSource(projectId, sourceId, currentUser);
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------ //
    // Internal helper
    // ------------------------------------------------------------------ //

    /**
     * Extracts the authenticated student's database ID from the
     * Spring Security context.
     *
     * @return the student's primary key ({@code users.id})
     */
    private Integer getAuthenticatedStudentId() {
        User currentUser = currentUserService.requireCurrentUser();
        return currentUser.getId();
    }
}
