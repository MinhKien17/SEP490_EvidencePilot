package com.evidencepilot.dto.response;

import com.evidencepilot.model.CollectionCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record CollectionCategoryResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) {
    public static CollectionCategoryResponse from(CollectionCategory category) {
        return new CollectionCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt());
    }
}
