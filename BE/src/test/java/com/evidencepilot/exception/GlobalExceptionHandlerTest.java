package com.evidencepilot.exception;

import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Disabled("Temporarily disabled pending post-AI pipeline refactor")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("exceptiontest@test.com");
        user.setPasswordHash("encoded-placeholder");
        user.setRole(UserRole.STUDENT);
        user.setFirstName("Exception");
        user.setLastName("Test");
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.saveAndFlush(user);

        bearerToken = "Bearer " + jwtUtils.generateToken(user);
    }

    @Test
    void getNonExistentProject_shouldReturn404WithApiErrorResponse() throws Exception {
        UUID missingUuid = UUID.randomUUID();

        mockMvc.perform(get("/api/projects/{id}", missingUuid)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", not(blankString())))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", not(blankString())))
                .andExpect(jsonPath("$.message", containsString(missingUuid.toString())))
                .andExpect(jsonPath("$.path", is("/api/projects/" + missingUuid)));
    }
}
