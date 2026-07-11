package com.evidencepilot.controller;

import com.evidencepilot.service.AiModelClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class HealthControllerTest {

    @Test
    void health_returnsBackendAndAiStatus() throws Exception {
        AiModelClient aiModelClient = mock(AiModelClient.class);
        when(aiModelClient.health()).thenReturn(Map.of("status", "ok"));
        MockMvc mockMvc = standaloneSetup(new HealthController(aiModelClient)).build();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.ai.status").value("ok"));

        verify(aiModelClient).health();
    }
}
