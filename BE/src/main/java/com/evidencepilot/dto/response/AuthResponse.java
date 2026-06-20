package com.evidencepilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after successful authentication.
 *
 * <p>Packages the signed JWT together with minimal user metadata so the
 * frontend can store the token and display role-based UI without a second
 * round-trip.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Signed JWT — send as {@code Authorization: Bearer <token>} on subsequent requests. */
    private String token;

    /** The authenticated user's e-mail address. */
    private String email;

    /** The authenticated user's role (STUDENT, INSTRUCTOR, ADMIN). */
    private String role;
}
