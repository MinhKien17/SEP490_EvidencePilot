package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CollectionRequest(
    @NotBlank @Size(max = 255) String name,
    String description,
    UUID projectId
) {}
