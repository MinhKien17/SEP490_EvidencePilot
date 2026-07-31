package com.evidencepilot.controller;

import com.evidencepilot.dto.response.GraphResponse;
import com.evidencepilot.service.GraphService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GraphControllerTest {

    @Test
    void graphDefaultsNetworkScopeToAll() throws Exception {
        GraphService service = mock(GraphService.class);
        UUID projectId = UUID.randomUUID();
        when(service.getGraph(projectId, "all")).thenReturn(new GraphResponse(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                false));
        MockMvc mockMvc = standaloneSetup(new GraphController(service)).build();

        mockMvc.perform(get("/api/projects/{projectId}/graph", projectId))
                .andExpect(status().isOk());

        verify(service).getGraph(projectId, "all");
    }

    @Test
    void claimStatsDelegatesToService() throws Exception {
        GraphService service = mock(GraphService.class);
        UUID projectId = UUID.randomUUID();
        when(service.getClaimStats(projectId))
                .thenReturn(new GraphResponse.ClaimStatsResponse(0, Map.of()));
        MockMvc mockMvc = standaloneSetup(new GraphController(service)).build();

        mockMvc.perform(get("/api/projects/{projectId}/graph/claim-stats", projectId))
                .andExpect(status().isOk());

        verify(service).getClaimStats(projectId);
    }
}
