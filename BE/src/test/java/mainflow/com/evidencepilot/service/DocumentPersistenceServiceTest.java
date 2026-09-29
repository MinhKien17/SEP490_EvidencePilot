package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.DocumentPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPersistenceServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentTextRepository documentTextRepository;
    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;
    @Mock private SystemNotificationService notifications;
    @Mock private UserRepository userRepository;

    @InjectMocks private DocumentPersistenceService service;

    @Test
    void uploadCommitsWhenAuditFails() {
        UUID id = UUID.randomUUID();
        Document document = new Document();
        document.setId(id);
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentRepository.save(document)).thenReturn(document);
        doThrow(new RuntimeException("audit store down")).when(auditService)
                .record(any(), any(), any(), any(), any(), any());

        Document saved = service.markDocumentAsUploaded(id, "file-key", "hash");

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getFileUrl()).isEqualTo("file-key");
        verify(documentRepository).save(document);
    }
}
