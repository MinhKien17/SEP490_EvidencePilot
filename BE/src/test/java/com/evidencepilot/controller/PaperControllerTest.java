package com.evidencepilot.controller;

import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.PaperProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PaperControllerTest {

    private final DocumentService documentService = mock(DocumentService.class);
    private final PaperProcessingService paperService = mock(PaperProcessingService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new PaperController(documentService, paperService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_delegatesToCurrentUserScope() throws Exception {
        mockMvc.perform(get("/api/papers")).andExpect(status().isOk());
        verify(documentService).getAllPapersForCurrentUser();
    }

    @Test
    void findById_returnsActivePaper() throws Exception {
        UUID id = UUID.randomUUID();
        DocumentResponse paper = mock(DocumentResponse.class);
        when(paper.docType()).thenReturn(DocumentType.PAPER);
        when(paper.active()).thenReturn(true);
        when(documentService.getDocumentById(id)).thenReturn(paper);

        mockMvc.perform(get("/api/papers/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void findById_rejectsSourceDocument() throws Exception {
        UUID id = UUID.randomUUID();
        DocumentResponse source = mock(DocumentResponse.class);
        when(source.docType()).thenReturn(DocumentType.SOURCE);
        when(source.active()).thenReturn(true);
        when(documentService.getDocumentById(id)).thenReturn(source);

        mockMvc.perform(get("/api/papers/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void findByProject_filtersInactiveAndSourceDocuments() throws Exception {
        UUID projectId = UUID.randomUUID();
        DocumentResponse paper = document(DocumentType.PAPER, true);
        DocumentResponse source = document(DocumentType.SOURCE, true);
        DocumentResponse inactivePaper = document(DocumentType.PAPER, false);
        when(documentService.getDocumentsByProject(projectId)).thenReturn(List.of(paper, source, inactivePaper));

        mockMvc.perform(get("/api/projects/{id}/papers", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void sections_delegatesPaperId() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/papers/{id}/sections", id)).andExpect(status().isOk());
        verify(paperService).getPaperSections(id);
    }

    @Test
    void review_bindsTargetStyle() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/papers/{id}/reviews", id).param("targetStyle", "APA"))
                .andExpect(status().isOk());
        verify(paperService).review(id, "APA");
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/papers/{id}", id)).andExpect(status().isNoContent());
        verify(documentService).deleteDocument(id);
    }

    @Test
    void upload_returns201WithoutDetectingSectionsSynchronously() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentResponse response = mock(DocumentResponse.class);
        when(response.id()).thenReturn(documentId);
        when(documentService.uploadDocument(eq(projectId), any(), eq(DocumentType.PAPER))).thenReturn(response);
        MockMultipartFile file = new MockMultipartFile("file", "paper.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/papers").file(file).param("projectId", projectId.toString()))
                .andExpect(status().isCreated());

        verify(documentService).uploadDocument(eq(projectId), any(), eq(DocumentType.PAPER));
        verify(paperService, never()).detectAndPersistSections(documentId);
    }

    private static DocumentResponse document(DocumentType type, boolean active) {
        DocumentResponse response = mock(DocumentResponse.class);
        when(response.docType()).thenReturn(type);
        when(response.active()).thenReturn(active);
        return response;
    }
}
