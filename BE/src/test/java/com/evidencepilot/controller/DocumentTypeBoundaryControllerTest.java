package com.evidencepilot.controller;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.PaperProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTypeBoundaryControllerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PaperSectionRepository paperSectionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PaperProcessingService paperProcessingService;

    @Mock
    private DocumentService documentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentTextRepository documentTextRepository;

    @Test
    void sourceEndpointDoesNotReturnStudentSubmission() {
        Document paper = document(DocumentType.PAPER);
        when(documentRepository.findById(paper.getId())).thenReturn(Optional.of(paper));

        SourceController controller = new SourceController(
                documentRepository,
                userRepository,
                documentChunkRepository,
                documentTextRepository,
                currentUserService,
                documentService);

        assertThatThrownBy(() -> controller.findById(paper.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void paperEndpointDoesNotReturnEvidenceSource() {
        Document source = document(DocumentType.SOURCE);
        when(documentRepository.findById(source.getId())).thenReturn(Optional.of(source));

        PaperController controller = new PaperController(
                documentRepository,
                paperSectionRepository,
                projectRepository,
                currentUserService,
                paperProcessingService,
                documentService);

        assertThatThrownBy(() -> controller.findById(source.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    private Document document(DocumentType type) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setDocType(type);
        document.setActive(true);
        return document;
    }
}
