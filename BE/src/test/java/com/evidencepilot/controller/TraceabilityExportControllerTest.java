package com.evidencepilot.controller;

import com.evidencepilot.service.TraceabilityExportService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TraceabilityExportControllerTest {

    @Test
    void export_delegatesProjectId() throws Exception {
        TraceabilityExportService service = mock(TraceabilityExportService.class);
        MockMvc mockMvc = standaloneSetup(new TraceabilityExportController(service)).build();
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(get("/api/projects/{projectId}/traceability", projectId))
                .andExpect(status().isOk());

        verify(service).exportTraceability(projectId);
    }
}
