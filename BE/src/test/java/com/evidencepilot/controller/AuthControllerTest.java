package com.evidencepilot.controller;

import com.evidencepilot.config.JwtUtil;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.dto.request.RegisterRequest;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtUtil jwtUtil = new JwtUtil("EvidencePilot-Test-Secret-Key-For-Jwt!!", 86_400_000L);
    private final AuthController controller =
            new AuthController(userRepository, new BCryptPasswordEncoder(), jwtUtil);

    @Test
    void registerCreatesRequestedRoleAndDoesNotExposePasswordHash() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Passw0rd!");
        request.setRole(UserRole.STUDENT);

        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7);
            assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
            assertThat(saved.getPasswordHash()).startsWith("$2");
            return saved;
        });

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", "student@example.com");
        assertThat(response.getBody()).containsEntry("role", UserRole.STUDENT.name());
        assertThat(response.getBody()).doesNotContainKey("passwordHash");
    }

    @Test
    void registerRejectsDuplicateEmailBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Passw0rd!");
        request.setRole(UserRole.STUDENT);

        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        assertThatThrownBy(() -> controller.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginTokenCarriesActualUserRole() {
        User user = new User();
        user.setId(7);
        user.setEmail("instructor@example.com");
        user.setRole(UserRole.INSTRUCTOR);
        user.setPasswordHash(new BCryptPasswordEncoder().encode("Passw0rd!"));

        when(userRepository.findByEmail("instructor@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("instructor@example.com");
        request.setPassword("Passw0rd!");

        var response = controller.login(request);

        assertThat(jwtUtil.extractRole(response.getBody().getToken()))
                .isEqualTo(UserRole.INSTRUCTOR.name());
    }
}
