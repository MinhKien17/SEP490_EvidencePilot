package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ClaimRequest;
import com.evidencepilot.service.ClaimEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RagControllerTest {

    private final ClaimEvaluationService service = mock(ClaimEvaluationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clearInvocations(service);
        mockMvc = standaloneSetup(new RagController(service)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/paper/{documentId}/claims/match", "/api/sources/{documentId}/claims/match"})
    void matchClaim_supportsPaperAndSourceRoutes(String route) throws Exception {
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(post(route, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claimText\":\"The control is implemented\"}"))
                .andExpect(status().isOk());

        verify(service).evaluate(eq(documentId), any(ClaimRequest.class));
    }
}
