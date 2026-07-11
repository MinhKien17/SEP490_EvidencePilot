package com.evidencepilot.controller;

import com.evidencepilot.dto.request.SourceCategoryRequest;
import com.evidencepilot.service.SourceCategoryService;
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

class SourceCategoryControllerTest {

    private final SourceCategoryService service = mock(SourceCategoryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new SourceCategoryController(service)).build();
    }

    @Test
    void activeCategories_delegatesToService() throws Exception {
        mockMvc.perform(get("/api/source-categories")).andExpect(status().isOk());
        verify(service).getActiveCategories();
    }

    @Test
    void adminCategories_bindsActiveFilter() throws Exception {
        mockMvc.perform(get("/api/admin/source-categories").param("active", "false"))
                .andExpect(status().isOk());
        verify(service).getCategories(false);
    }

    @Test
    void create_returns201() throws Exception {
        mockMvc.perform(post("/api/admin/source-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Standard\",\"description\":\"Primary evidence\"}"))
                .andExpect(status().isCreated());
        verify(service).create(any(SourceCategoryRequest.class));
    }

    @Test
    void create_rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/admin/source-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void update_bindsIdBodyAndActive() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/api/admin/source-categories/{id}", id)
                        .param("active", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
        verify(service).update(eq(id), any(SourceCategoryRequest.class), eq(true));
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/admin/source-categories/{id}", id))
                .andExpect(status().isNoContent());
        verify(service).delete(id);
    }
}
