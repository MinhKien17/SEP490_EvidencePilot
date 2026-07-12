package com.evidencepilot.service;

import com.evidencepilot.dto.ExtractionRequest;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.impl.SourceExtractionServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SourceExtractionServiceImplTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final SourceExtractionServiceImpl service = new SourceExtractionServiceImpl(documents, rabbit);

    @Test
    void triggerExtractionMarksQueuedAndPublishesRequest() {
        UUID id = UUID.randomUUID();
        Document document = document(id);
        when(documents.findById(id)).thenReturn(Optional.of(document));

        service.triggerExtraction(id);

        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.QUEUED);
        var captor = ArgumentCaptor.forClass(ExtractionRequest.class);
        verify(rabbit).convertAndSend(eq("extraction.queue"), captor.capture());
        assertThat(captor.getValue().documentId()).isEqualTo(id);
    }

    @Test
    void triggerExtractionMarksFailedWhenPublishFails() {
        UUID id = UUID.randomUUID();
        Document document = document(id);
        when(documents.findById(id)).thenReturn(Optional.of(document));
        org.mockito.Mockito.doThrow(new AmqpException("offline"))
                .when(rabbit).convertAndSend(eq("extraction.queue"), any(ExtractionRequest.class));

        service.triggerExtraction(id);

        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(document.getProcessingError()).isEqualTo("Failed to queue extraction");
    }

    @Test
    void triggerExtractionRejectsMissingDocument() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.triggerExtraction(id)).hasMessageContaining(id.toString());
        verifyNoInteractions(rabbit);
    }

    private static Document document(UUID id) {
        Document document = new Document();
        document.setId(id);
        return document;
    }
}
