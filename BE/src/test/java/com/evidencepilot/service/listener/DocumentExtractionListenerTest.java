package com.evidencepilot.service.listener;

import com.evidencepilot.service.DocumentExtractionWorker;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DocumentExtractionListenerTest {

    @Test
    void handleExtractionJobAcceptsQuotedDocumentId() {
        DocumentExtractionWorker worker = mock(DocumentExtractionWorker.class);
        DocumentExtractionListener listener = new DocumentExtractionListener(worker);
        UUID documentId = UUID.randomUUID();

        listener.handleExtractionJob("\"" + documentId + "\"");

        verify(worker).process(documentId);
    }

    @Test
    void handleExtractionJobRejectsEmptyBody() {
        DocumentExtractionWorker worker = mock(DocumentExtractionWorker.class);
        DocumentExtractionListener listener = new DocumentExtractionListener(worker);

        assertThrows(IllegalArgumentException.class, () -> listener.handleExtractionJob("   "));

        verifyNoInteractions(worker);
    }
}
