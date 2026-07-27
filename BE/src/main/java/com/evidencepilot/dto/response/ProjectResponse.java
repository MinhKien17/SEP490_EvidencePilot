package com.evidencepilot.dto.response;

import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.model.enums.ProjectStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String title,
    String description,
    ProjectStatus status,
    PaperStandard targetStandard,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getTitle(),
            project.getDescription(),
            project.getStatus(),
            project.getTargetStandard(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
