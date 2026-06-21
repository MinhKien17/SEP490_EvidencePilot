package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.Dataset;
import java.time.LocalDateTime;

public record DatasetResponseDto(
        Integer id,
        Integer instructorId,
        String title,
        boolean active,
        LocalDateTime createdAt
) {
    public static DatasetResponseDto fromEntity(Dataset dataset) {
        if (dataset == null) return null;
        return new DatasetResponseDto(
                dataset.getId(),
                dataset.getInstructor() != null ? dataset.getInstructor().getId() : null,
                dataset.getTitle(),
                dataset.isActive(),
                dataset.getCreatedAt()
        );
    }
}
