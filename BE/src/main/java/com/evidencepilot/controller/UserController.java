package com.evidencepilot.controller;

import com.evidencepilot.config.JwtUtil;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.dto.LoginRequest;
import com.evidencepilot.dto.RegisterRequest;
import com.evidencepilot.dto.UserResponse;
import com.evidencepilot.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller for User CRUD operations.
 *
 * <p><b>Password hashing:</b> every create and update call passes the
 * incoming plaintext password through {@link PasswordEncoder} (BCrypt,
 * strength 12) before the value ever reaches the database.  The raw
 * plaintext is never logged or persisted.</p>
 *
 * <p>Base path: {@code /api/users}</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;   // BCrypt from PasswordConfig
    private final JwtUtil          jwtUtil;           // JWT signing/validation

    // ── Read ───────────────────────────────────────────────────────────────────

    @GetMapping
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + id));
    }

    // ── Create ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new user.
     *
     * <p>The caller supplies a plaintext password in the {@code passwordHash}
     * field of the request body.  This endpoint replaces it with its BCrypt
     * hash before saving — the plaintext is not retained anywhere.</p>
     *
     * <p>Example request body:
     * <pre>
     * {
     *   "email": "alice@example.com",
     *   "passwordHash": "myPlaintextPassword",
     *   "role": "STUDENT"
     * }
     * </pre>
     * </p>
     *
     * @return 201 Created with a safe user response.
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody User user) {
        guardPassword(user);
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email must not be blank.");
        }
        if (user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must not be blank.");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already exists.");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

    /**
     * Registration alias — identical to {@link #create} but lives at
     * {@code POST /api/users/register} so the frontend can call a semantic URL.
     *
     * <p>Request body: {@code { "email", "password" }}</p>
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already exists.");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPassword());
        user.setRole(UserRole.STUDENT);
        guardPassword(user);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    /**
     * Login endpoint — validates credentials and returns a signed JWT.
     *
     * <p>Request body: {@code { "email": "...", "password": "<plaintext>" }}<br>
     * Response:       {@code { "token": "<JWT>", "id": ..., "email": "...", "role": "..." }}</p>
     *
     * <p>The plaintext password is compared against the BCrypt hash stored in
     * the database using {@link PasswordEncoder#matches}.  On success a real
     * signed JWT is returned; the frontend should store it and send it as
     * {@code Authorization: Bearer <token>} on subsequent requests.</p>
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String email       = request.getEmail();
        String rawPassword = request.getPassword();

        if (email == null || rawPassword == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "email and password are required");
        }

        // 1. Look up the user — return 401 (not 404) to avoid user enumeration
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // 2. Compare plaintext password against the stored BCrypt hash
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // 3. Issue a signed JWT — valid for 24 hours
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "id",    user.getId(),
                "email", user.getEmail(),
                "role",  user.getRole().name()
        ));
    }

    /**
     * Returns the currently authenticated user's profile.
     *
     * <p>The JWT filter already validated the token and populated the
     * {@code SecurityContextHolder}; here we simply extract the e-mail
     * claim from the token to look up the full user record.</p>
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        String email = jwtUtil.extractEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "role",  user.getRole().name()
        ));
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
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Integer id, @RequestBody User user) {
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

        return UserResponse.from(userRepository.save(user));
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

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
