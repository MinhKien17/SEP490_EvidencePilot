package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.event.DocumentUploadedEvent;
import com.evidencepilot.service.impl.DocumentPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DocumentPersistenceServiceTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final DocumentPersistenceService service = new DocumentPersistenceService(documents, events);

    @Test
    void savePendingDocument_populatesUploadMetadata() {
        Project project = new Project();
        User user = new User();
        when(documents.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document saved = service.savePendingDocument(
                project, null, user, DocumentType.SOURCE, "source.pdf", "application/pdf", 12L);

        assertThat(saved.getProject()).isSameAs(project);
        assertThat(saved.getUploadedBy()).isSameAs(user);
        assertThat(saved.getDocType()).isEqualTo(DocumentType.SOURCE);
        assertThat(saved.getFileUrl()).isEqualTo("pending");
        assertThat(saved.getProcessingStatus()).isEqualTo(ProcessingStatus.PENDING_UPLOAD);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void markDocumentAsUploaded_updatesStatusAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        Document document = new Document();
        document.setId(id);
        when(documents.findById(id)).thenReturn(Optional.of(document));
        when(documents.save(document)).thenReturn(document);

        Document saved = service.markDocumentAsUploaded(id, "sources/raw/file.pdf");

        assertThat(saved.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
        assertThat(saved.getFileUrl()).isEqualTo("sources/raw/file.pdf");
        verify(events).publishEvent(new DocumentUploadedEvent(id));
    }

    @Test
    void markDocumentAsUploaded_rejectsMissingDocument() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.markDocumentAsUploaded(id, "key"))
                .hasMessageContaining(id.toString());
        verifyNoInteractions(events);
    }
}
