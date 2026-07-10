package com.evidencepilot.dto.response;

import com.evidencepilot.model.SourceCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record SourceCategoryResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) {
    public static SourceCategoryResponse from(SourceCategory category) {
        return new SourceCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt());
    }
}
