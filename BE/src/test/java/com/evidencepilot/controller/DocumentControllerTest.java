package com.evidencepilot.controller;

import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.dto.ExtractionRequest;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.UserRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private MinioClient minioClient;

    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        User user = new User();
        user.setEmail("docuploader@test.com");
        user.setPasswordHash("encoded-placeholder");
        user.setRole(UserRole.STUDENT);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.saveAndFlush(user);

        bearerToken = "Bearer " + jwtUtils.generateToken(user);
    }

    @AfterEach
    void cleanUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void uploadDocument_shouldReturn202AndPublishExtractionJob() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-doc.pdf", "application/pdf", "fake-pdf-content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .header("Authorization", bearerToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", matchesPattern(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
                .andExpect(jsonPath("$.originalFilename", is("test-doc.pdf")))
                .andExpect(jsonPath("$.active", is(true)));

        verify(minioClient).putObject(any(PutObjectArgs.class));

        long count = documentRepository.count();
        assertEquals(1, count, "Exactly one document should be in the database");

        var saved = documentRepository.findAll().iterator().next();
        assertNotNull(saved.getId());
        assertEquals("test-doc.pdf", saved.getOriginalFilename());

        var captor = ArgumentCaptor.forClass(ExtractionRequest.class);
        verify(rabbitTemplate).convertAndSend(eq("extraction.queue"), captor.capture());
        ExtractionRequest payload = captor.getValue();
        assertEquals(saved.getId(), payload.documentId());
    }

    @Test
    void uploadDocument_withoutFile_shouldReturn400() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());

        assertEquals(0, documentRepository.count());
        verifyNoInteractions(minioClient, rabbitTemplate);
    }

    @Test
    void uploadDocument_withEmptyFile_shouldReturn400WithoutPersistingOrPublishing() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/documents")
                        .file(emptyFile)
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());

        assertEquals(0, documentRepository.count());
        verifyNoInteractions(minioClient, rabbitTemplate);
    }
}
