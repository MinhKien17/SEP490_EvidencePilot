package com.evidencepilot.controller;

import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.EmailVerificationService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void register_shouldCreateUnverifiedStudentWithoutIssuingToken() throws Exception {
        when(emailVerificationService.createVerificationToken(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setEmailVerified(false);
            user.setEmailVerificationTokenHash("hashed-token");
            user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
            return "raw-token";
        });

        String body = """
                {
                    "email": "newuser@test.com",
                    "password": "StrongPass1!",
                    "firstName": "Jane",
                    "lastName": "Doe"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", blankOrNullString()))
                .andExpect(jsonPath("$.user.id", matchesPattern(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
                .andExpect(jsonPath("$.user.email", is("newuser@test.com")))
                .andExpect(jsonPath("$.user.role", is("STUDENT")));

        User user = userRepository.findByEmail("newuser@test.com").orElseThrow();
        verify(emailVerificationService).sendVerificationEmail(eq(user), eq("raw-token"));
    }

    @Test
    void login_shouldReturnValidJwtForVerifiedUser() throws Exception {
        String email = "loginuser@test.com";
        String rawPassword = "ValidPass1!";

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.INSTRUCTOR);
        user.setEmailVerified(true);
        user.setFirstName("John");
        user.setLastName("Smith");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.saveAndFlush(user);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(rawPassword);
        String body = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(request);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.role", is("INSTRUCTOR")));
    }

    @Test
    void login_shouldRejectUnverifiedEmail() throws Exception {
        User user = new User();
        user.setEmail("student@test.com");
        user.setPasswordHash(passwordEncoder.encode("ValidPass1!"));
        user.setRole(UserRole.STUDENT);
        user.setEmailVerified(false);
        userRepository.saveAndFlush(user);

        String body = """
                {
                    "email": "student@test.com",
                    "password": "ValidPass1!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyEmail_shouldReturnVerifiedEmail() throws Exception {
        when(emailVerificationService.verifyEmail("raw-token")).thenReturn("student@test.com");

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "raw-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Email verified successfully")))
                .andExpect(jsonPath("$.email", is("student@test.com")));
    }
}
