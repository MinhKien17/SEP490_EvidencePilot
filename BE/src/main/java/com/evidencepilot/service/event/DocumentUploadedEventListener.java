package com.evidencepilot.service.event;

import com.evidencepilot.service.SourceExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentUploadedEventListener {

    private final SourceExtractionService sourceExtractionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        log.info("Document {} upload committed, triggering extraction", event.documentId());
        sourceExtractionService.triggerExtraction(event.documentId());
    }
}
