package com.evidencepilot.controller;

import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SourceControllerTest {

    private final DocumentService service = mock(DocumentService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new SourceController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findById_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/sources/{id}", id)).andExpect(status().isOk());
        verify(service).getSourceById(id);
    }

    @Test
    void chunks_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/sources/{id}/chunks", id)).andExpect(status().isOk());
        verify(service).getDocumentChunks(id);
    }

    @Test
    void text_delegatesId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/sources/{id}/text", id)).andExpect(status().isOk());
        verify(service).getDocumentText(id);
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/sources/{id}", id)).andExpect(status().isNoContent());
        verify(service).deleteDocument(id);
    }

    @Test
    void upload_bindsAllOptionalScopes() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "source.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/sources").file(file)
                        .param("projectId", projectId.toString())
                        .param("collectionId", collectionId.toString())
                        .param("sourceCategoryId", categoryId.toString()))
                .andExpect(status().isCreated());

        verify(service).uploadDocument(
                eq(projectId), eq(collectionId), eq(categoryId), any(), eq(DocumentType.SOURCE));
    }

    @Test
    void upload_rejectsInvalidCategoryId() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "source.pdf", "application/pdf", "pdf".getBytes());
        mockMvc.perform(multipart("/api/sources").file(file).param("sourceCategoryId", "bad"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
