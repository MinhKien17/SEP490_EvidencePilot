package com.evidencepilot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for self-service profile updates ({@code PUT /api/users/me}).
 *
 * <p>Only non-sensitive profile fields are accepted.  Password and role
 * changes are handled through dedicated, separately authorised endpoints
 * to enforce the principle of least privilege.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Min(value = 1, message = "Age must be a positive number")
    private Integer age;
}
