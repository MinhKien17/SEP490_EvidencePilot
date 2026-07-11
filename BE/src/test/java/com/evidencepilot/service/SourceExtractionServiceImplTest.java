package com.evidencepilot.service;

import com.evidencepilot.dto.ExtractionRequest;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.impl.SourceExtractionServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SourceExtractionServiceImplTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final SourceExtractionServiceImpl service = new SourceExtractionServiceImpl(documents, rabbit);

    @Test
    void triggerExtraction_marksProcessingAndPublishesExtractionRequest() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Document document = new Document();
        document.setId(id);
        document.setFileUrl("sources/raw/test.pdf");
        document.setUploadedBy(user);
        when(documents.findById(id)).thenReturn(Optional.of(document));

        service.triggerExtraction(id);

        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSING);
        verify(documents).save(document);

        var captor = ArgumentCaptor.forClass(ExtractionRequest.class);
        verify(rabbit).convertAndSend(
                eq("evidence.exchange"), eq("document.extract"), captor.capture());
        ExtractionRequest payload = captor.getValue();
        assertThat(payload.documentId()).isEqualTo(id);
        assertThat(payload.s3ObjectKey()).isEqualTo("sources/raw/test.pdf");
        assertThat(payload.userId()).isEqualTo(userId);
    }

    @Test
    void triggerExtraction_rejectsMissingDocument() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.triggerExtraction(id)).hasMessageContaining(id.toString());
        verifyNoInteractions(rabbit);
    }

    @Test
    void extractText_usesRawAndPdfStrategies() {
        var raw = new MockMultipartFile("file", "notes.txt", "text/plain", "plain text".getBytes());
        var pdf = new MockMultipartFile("file", "paper.pdf", "application/pdf", "pdf text with spaces".getBytes());

        assertThat(service.extractText(raw)).satisfies(result -> {
            assertThat(result.text()).isEqualTo("plain text");
            assertThat(result.method()).isEqualTo("raw");
        });
        assertThat(service.extractText(pdf)).satisfies(result -> {
            assertThat(result.text()).isEqualTo("pdf text with spaces");
            assertThat(result.method()).isEqualTo("pdfbox");
        });
    }

    @Test
    void extractText_returnsFailedResultOnIoError() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("broken.txt");
        when(file.getBytes()).thenThrow(new IOException("broken"));

        assertThat(service.extractText(file)).satisfies(result -> {
            assertThat(result.text()).isEmpty();
            assertThat(result.method()).isEqualTo("failed");
        });
    }
}
