package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.request.UpdateProfileRequest;
import com.evidencepilot.dto.response.UserDto;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for User resource management.
 *
 * <p>All endpoints under {@code /api/users} require authentication.
 * Administrative CRUD operations (list, get-by-id, update, delete)
 * are restricted to the {@code ADMIN} role via {@code SecurityConfig}.</p>
 *
 * <p>Base path: {@code /api/users}</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile self-service and admin user management")
public class UserController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Self-service ──────────────────────────────────────────────────────────

    /**
     * Returns the currently authenticated user's profile.
     *
     * <p>The JWT filter populates the {@code SecurityContextHolder} with
     * the user's e-mail as the principal; this endpoint resolves it to a
     * safe {@link UserDto} projection (no password hash, no internal IDs
     * that should stay private).</p>
     *
     * @return 200 OK with the {@link UserDto}
     */
    @Operation(
            summary = "Get my profile",
            description = "Returns the currently authenticated user's profile as a UserDto. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ANY")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(toDto(user));
    }

    /**
     * Self-service profile update for the currently authenticated user.
     *
     * <p>Only non-sensitive fields ({@code firstName}, {@code lastName},
     * {@code age}) are accepted via {@link UpdateProfileRequest}.
     * Password and role changes are handled through dedicated endpoints
     * to prevent privilege escalation.</p>
     *
     * @param request validated profile-update payload
     * @return 200 OK with the updated {@link UserDto}
     */
    @Operation(
            summary = "Update my profile",
            description = "Updates the authenticated user's non-sensitive profile fields "
                        + "(firstName, lastName, age). Password and role are excluded. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ANY")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — invalid field values"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(
            @Valid @RequestBody UpdateProfileRequest request) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        // Selective field-by-field mapping — only profile data, never credentials
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAge(request.getAge());

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toDto(saved));
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    @Operation(
            summary = "List all users",
            description = "Returns every user in the system as a list of UserDto projections. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller is not ADMIN")
    })
    @GetMapping
    public List<UserDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user's profile by their numeric ID. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller is not ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public UserDto findById(@PathVariable Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + id));
        return toDto(user);
    }

    // ── Update ─────────────────────────────────────────────────────────────────

    /**
     * Fully replaces an existing user.
     *
     * <p>If the {@code passwordHash} field in the request body differs from
     * the currently stored hash (i.e. the caller is changing the password),
     * it is re-hashed before saving.  If the caller echoes back the existing
     * BCrypt hash verbatim, it is left untouched to prevent double-hashing.</p>
     */
    @Operation(
            summary = "Update user by ID",
            description = "Fully replaces an existing user record. Re-hashes the password only "
                        + "if a new plaintext value is supplied. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Password must not be blank"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller is not ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    public UserDto update(@PathVariable Integer id, @RequestBody User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + id));

        user.setId(id);

        // Re-hash only when a new plaintext password is provided.
        // A BCrypt hash always starts with "$2a$" (or "$2b$"); if the caller
        // sends back the existing hash we leave it unchanged to prevent
        // double-hashing which would permanently invalidate the password.
        String incomingPassword = user.getPasswordHash();
        guardPassword(user);
        if (!incomingPassword.equals(existing.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(incomingPassword));
        } else {
            // Caller echoed the stored hash — keep it as-is
            user.setPasswordHash(existing.getPasswordHash());
        }

        return toDto(userRepository.save(user));
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Delete user by ID",
            description = "Permanently removes a user from the system. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller is not ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Converts a {@link User} entity to a safe {@link UserDto} projection,
     * stripping the password hash and any other internal-only fields.
     */
    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .age(user.getAge())
                .build();
    }

    /**
     * Rejects requests where the password field is missing or blank before
     * any hashing or DB operation takes place.
     */
    private void guardPassword(User user) {
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must not be blank.");
        }
    }
}
