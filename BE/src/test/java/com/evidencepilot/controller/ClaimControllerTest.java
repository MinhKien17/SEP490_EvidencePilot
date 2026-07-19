package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ClaimControllerTest {

    private final ClaimService service = mock(ClaimService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ClaimController(service)).build();
    }

    @Test
    void getAllClaims_bindsPagingAndFilters() throws Exception {
        mockMvc.perform(get("/api/claims").param("page", "2").param("size", "10")
                        .param("sort", "content,asc").param("q", "control").param("active", "false"))
                .andExpect(status().isOk());
        verify(service).getAllClaims(2, 10, "content,asc", "control", false);
    }

    @Test
    void getClaimById_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/claims/{id}", id)).andExpect(status().isOk());
        verify(service).getClaimById(id);
    }

    @Test
    void createClaim_returns201() throws Exception {
        UUID sectionId = UUID.randomUUID();
        mockMvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionId\":\"" + sectionId + "\",\"content\":\"A supported claim\",\"aiConfidenceScore\":0.8}"))
                .andExpect(status().isCreated());
        verify(service).createClaim(any(ClaimCreationRequest.class));
    }

    @Test
    void updateClaim_bindsBodyValues() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/api/claims/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Updated\",\"aiConfidenceScore\":0.75}"))
                .andExpect(status().isOk());
        verify(service).updateClaim(id, "Updated", 0.75f);
    }

    @Test
    void deleteClaim_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/claims/{id}", id)).andExpect(status().isNoContent());
        verify(service).deleteClaim(id);
    }

    @Test
    void getSuggestions_delegatesClaimId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/claims/{id}/suggestions", id)).andExpect(status().isOk());
        verify(service).getSuggestionsForClaim(id);
    }

    @Test
    void createSuggestion_returns201AndBindsParameters() throws Exception {
        UUID claimId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        mockMvc.perform(post("/api/claims/{id}/suggestions", claimId)
                        .param("documentChunkId", chunkId.toString())
                        .param("score", "0.9").param("explanation", "Strong match"))
                .andExpect(status().isCreated());
        verify(service).createSuggestion(claimId, chunkId, 0.9f, "Strong match");
    }

    @Test
    void updateSuggestionStatus_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/claims/suggestions/{id}/status", id).param("status", "ACCEPTED"))
                .andExpect(status().isNoContent());
        verify(service).updateSuggestionStatus(id, "ACCEPTED");
    }

    @Test
    void getMappings_delegatesClaimId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/claims/{id}/mappings", id)).andExpect(status().isOk());
        verify(service).getMappingsForClaim(id);
    }

}
