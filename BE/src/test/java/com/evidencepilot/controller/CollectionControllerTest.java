package com.evidencepilot.controller;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.service.CollectionService;
import com.evidencepilot.service.DocumentService;
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

class CollectionControllerTest {

    private final CollectionService collectionService = mock(CollectionService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new CollectionController(collectionService, documentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCollection_returns201() throws Exception {
        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Evidence library\"}"))
                .andExpect(status().isCreated());
        verify(collectionService).createCollection(any(CollectionRequest.class));
    }

    @Test
    void getCollectionById_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/collections/{id}", id)).andExpect(status().isOk());
        verify(collectionService).getCollectionById(id);
    }

    @Test
    void getCollectionSources_bindsOptionalCategory() throws Exception {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        mockMvc.perform(get("/api/collections/{id}/sources", id)
                        .param("sourceCategoryId", categoryId.toString()))
                .andExpect(status().isOk());
        verify(documentService).getSourcesByCollection(id, categoryId);
    }

    @Test
    void getCollectionSources_rejectsInvalidCategoryId() throws Exception {
        mockMvc.perform(get("/api/collections/{id}/sources", UUID.randomUUID())
                        .param("sourceCategoryId", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void deleteCollection_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/collections/{id}", id)).andExpect(status().isNoContent());
        verify(collectionService).deleteCollection(id);
    }
}
