package com.evidencepilot.service;

import com.evidencepilot.config.security.JwtSessionRegistry;
import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.dto.request.RegisterRequest;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final EmailVerificationService verification = mock(EmailVerificationService.class);
    private final JwtSessionRegistry registry = new JwtSessionRegistry();
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(users, encoder, jwtUtils, verification, registry);
    }

    @Test
    void register_createsUnverifiedStudentAndSendsToken() {
        RegisterRequest request = registerRequest();
        when(encoder.encode("StrongPass1!")).thenReturn("hash");
        when(verification.createVerificationToken(any(User.class))).thenReturn("raw-token");

        var response = service.register(request);

        assertThat(response.getToken()).isNull();
        verify(users).save(argThat(user -> user.getRole() == UserRole.STUDENT
                && user.getPasswordHash().equals("hash")));
        verify(verification).sendVerificationEmail(any(User.class), eq("raw-token"));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = registerRequest();
        when(users.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(users, never()).save(any());
    }

    @Test
    void login_returnsJwtForVerifiedCredentials() {
        User user = user(true);
        LoginRequest request = loginRequest();
        when(users.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateToken(user)).thenReturn("jwt");

        assertThat(service.login(request).getToken()).isEqualTo("jwt");
    }

    @Test
    void login_rejectsUnknownOrWrongCredentials() {
        LoginRequest request = loginRequest();
        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");

        User user = user(true);
        when(users.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void login_rejectsUnverifiedEmail() {
        User user = user(false);
        LoginRequest request = loginRequest();
        when(users.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void verifyEmail_delegatesToken() {
        when(verification.verifyEmail("token")).thenReturn("user@test.com");
        assertThat(service.verifyEmail("token")).isEqualTo("user@test.com");
    }

    @Test
    void refresh_revokesOldJtiAndIssuesNewToken() {
        User user = user(true);
        user.setId(UUID.randomUUID());
        when(jwtUtils.validateToken("old")).thenReturn(true);
        when(jwtUtils.extractJti("old")).thenReturn("jti-old");
        when(jwtUtils.extractJti("new")).thenReturn("jti-new");
        when(jwtUtils.extractUserId("old")).thenReturn(user.getId());
        when(jwtUtils.generateToken(user)).thenReturn("new");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        registry.register("jti-old");

        var response = service.refresh("old");

        assertThat(response.getToken()).isEqualTo("new");
        assertThat(registry.isValid("jti-old")).isFalse();
        assertThat(registry.isValid("jti-new")).isTrue();
    }

    @Test
    void refresh_rejectsRevokedToken() {
        when(jwtUtils.validateToken("old")).thenReturn(true);
        when(jwtUtils.extractJti("old")).thenReturn("jti-old");

        assertThatThrownBy(() -> service.refresh("old"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void refresh_rejectsInvalidToken() {
        when(jwtUtils.validateToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> service.refresh("bad"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    private static RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@test.com");
        request.setPassword("StrongPass1!");
        request.setFirstName("Test");
        request.setLastName("User");
        return request;
    }

    private static LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("StrongPass1!");
        return request;
    }

    private static User user(boolean verified) {
        User user = new User();
        user.setEmail("user@test.com");
        user.setPasswordHash("hash");
        user.setRole(UserRole.STUDENT);
        user.setEmailVerified(verified);
        return user;
    }
}
