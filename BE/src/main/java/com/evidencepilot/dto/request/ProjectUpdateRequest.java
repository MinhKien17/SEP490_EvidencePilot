package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing project.
 *
 * <p>
 * Only {@code title} and {@code description} are mutable by the client.
 * Fields like {@code status} and {@code active} are managed exclusively
 * through dedicated server-side operations.
 * </p>
 */
public record ProjectUpdateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 65535, message = "Description must not exceed 65535 characters")
        String description
) {
}
