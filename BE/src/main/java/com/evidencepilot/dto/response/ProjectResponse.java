package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.enums.ProjectStatus;

import java.time.LocalDateTime;

/**
 * Flattened response payload for a project.
 *
 * <p>
 * Deliberately omits the {@code studentId} to avoid exposing internal
 * user identifiers. The authenticated student already knows they own
 * the project because it was returned from their tenant-scoped query.
 * </p>
 */
public record ProjectResponse(
        Integer id,
        String title,
        String description,
        ProjectStatus status,
        boolean active,
        LocalDateTime createdAt
) {

    /**
     * Maps a JPA {@link Project} entity to a response DTO.
     *
     * @param project the persisted entity (must not be {@code null})
     * @return a new {@code ProjectResponse} containing only client-safe fields
     */
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                project.isActive(),
                project.getCreatedAt()
        );
    }
}
