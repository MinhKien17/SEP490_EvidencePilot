package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull UserRole role,
        @Size(max = 50) String studentCode,
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password,
        boolean verifyEmail) {

    /**
     * Password and verify-email are mutually exclusive: an admin either sets
     * the password now or sends a set-password verification link — never both.
     */
    @AssertTrue(message = "Password must be omitted when verifyEmail is true, and required otherwise")
    public boolean isPasswordPolicyConsistent() {
        if (verifyEmail) {
            return password == null || password.isBlank();
        }
        return password != null && !password.isBlank();
    }
}
