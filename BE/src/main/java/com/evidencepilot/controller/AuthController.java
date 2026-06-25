package com.evidencepilot.controller;

import com.evidencepilot.config.JwtUtil;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.dto.request.RegisterRequest;
import com.evidencepilot.dto.request.UpdatePasswordRequest;
import com.evidencepilot.dto.response.AuthResponse;
import com.evidencepilot.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * REST controller responsible exclusively for authentication concerns:
 * registration, login, and password management.
 *
 * <p>Base path: {@code /api/auth}</p>
 *
 * <p><b>Password hashing:</b> every write path passes the incoming
 * plaintext password through {@link PasswordEncoder} (BCrypt, strength 12)
 * before the value ever reaches the database.  The raw plaintext is never
 * logged or persisted.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public registration & login, and authenticated password management")
public class AuthController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;
    
    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Public registration endpoint.
     *
     * <p>Accepts a {@link RegisterRequest} DTO, manually maps each field to
     * a new {@link User} entity (no mass assignment), hashes the password,
     * and persists the user.  Returns 200 OK with a confirmation message —
     * never the entity itself (to avoid leaking the hash).</p>
     *
     * @param request validated registration payload
     * @return 200 OK with a simple confirmation map
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided email, password, and role. "
                        + "**Security:** Public — no token required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields"),
            @ApiResponse(responseCode = "409", description = "Conflict — email is already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {

        // Guard against duplicate e-mail
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        // Explicit field-by-field mapping — prevents mass assignment
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        // createdAt is set automatically by @CreationTimestamp

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "email",   user.getEmail(),
                "role",    user.getRole().name()
        ));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Login endpoint — validates credentials and returns a signed JWT
     * wrapped in an {@link AuthResponse}.
     *
     * <p>The role is injected as a custom claim into the JWT so the
     * {@link com.evidencepilot.config.JwtAuthenticationFilter} can later
     * reconstruct the correct {@code GrantedAuthority}.</p>
     *
     * @param request validated login payload
     * @return 200 OK with the {@link AuthResponse} containing the token
     */
    @Operation(
            summary = "Authenticate user",
            description = "Validates email/password credentials and returns a signed JWT (valid 24 h). "
                        + "**Security:** Public — no token required.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned in response body"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String email       = request.getEmail();
        String rawPassword = request.getPassword();

        // 1. Look up the user — return 401 (not 404) to avoid user enumeration
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // 2. Compare plaintext password against the stored BCrypt hash
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // 3. Issue a signed JWT with the role claim — valid for 24 hours
        String roleName = user.getRole().name();
        String token = jwtUtil.generateToken(user.getEmail(), roleName);

        // 4. Return the typed AuthResponse DTO
        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(roleName)
                .build();

        return ResponseEntity.ok(authResponse);
    }

    // ── Password Update ───────────────────────────────────────────────────────

    /**
     * Authenticated password-change endpoint.
     *
     * <p>Extracts the caller's identity from the {@code SecurityContext}
     * (populated by the JWT filter), verifies the old password against
     * the stored BCrypt hash, and persists the new hash.</p>
     *
     * @param request validated payload with {@code oldPassword} and {@code newPassword}
     * @return 200 OK with a confirmation message
     */
    @Operation(
            summary = "Update password",
            description = "Changes the authenticated user's password. The caller must supply the current "
                        + "password for verification. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ANY")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Current password is incorrect, or validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/update-password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request) {

        // 1. Extract the authenticated user's email from the SecurityContext
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        // 2. Verify the old password matches the stored hash
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Current password is incorrect");
        }

        // 3. Hash the new password and persist
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
