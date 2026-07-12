package com.evidencepilot.service;

import java.util.UUID;

public interface SourceExtractionService {

    void triggerExtraction(UUID documentId);
}
