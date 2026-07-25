package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CollectionResponse(
    UUID id,
    String name,
    String description,
    UUID categoryId,
    String categoryName,
    UUID projectId,
    LocalDateTime createdAt
) {}
