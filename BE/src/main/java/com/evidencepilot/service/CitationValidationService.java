package com.evidencepilot.service;

import com.evidencepilot.dto.response.CitationValidationResponse;
import java.util.UUID;

public interface CitationValidationService {
    CitationValidationResponse validateCitations(UUID documentId);
}
