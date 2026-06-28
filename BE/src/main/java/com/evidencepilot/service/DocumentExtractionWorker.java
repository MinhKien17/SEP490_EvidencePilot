package com.evidencepilot.service;

import java.util.UUID;

public interface DocumentExtractionWorker {

    void process(UUID documentId);

    class DocumentExtractionException extends RuntimeException {
        public DocumentExtractionException(String message) {
            super(message);
        }
    }
}
