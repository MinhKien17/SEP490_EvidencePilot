package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new project.
 *
 * <p>
 * Deliberately excludes {@code studentId}, {@code status}, and {@code active}.
 * The owner is extracted from the Spring Security context; status defaults
 * to {@code DRAFT}; and active defaults to {@code true} — all server-side.
 * </p>
 */
public record ProjectCreateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 65535, message = "Description must not exceed 65535 characters")
        String description
) {
}
