package com.evidencepilot.controller;

import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.Disabled;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Disabled("Temporarily disabled pending post-AI pipeline refactor")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void register_shouldReturnAuthResponseWithValidUuid() throws Exception {
        String body = """
                {
                    "email": "newuser@test.com",
                    "password": "StrongPass1!",
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "role": "STUDENT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankString())))
                .andExpect(jsonPath("$.user.id", matchesPattern(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
                .andExpect(jsonPath("$.user.email", is("newuser@test.com")))
                .andExpect(jsonPath("$.user.role", is("STUDENT")));
    }

    @Test
    void login_shouldReturnValidJwt() throws Exception {
        String email = "loginuser@test.com";
        String rawPassword = "ValidPass1!";

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.INSTRUCTOR);
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
                .andExpect(jsonPath("$.token", not(blankString())))
                .andExpect(jsonPath("$.user.id", matchesPattern(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.role", is("INSTRUCTOR")));
    }
}
