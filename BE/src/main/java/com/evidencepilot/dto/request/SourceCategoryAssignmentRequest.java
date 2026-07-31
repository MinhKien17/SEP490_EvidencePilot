package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SourceCategoryAssignmentRequest(@NotNull UUID categoryId) {}
