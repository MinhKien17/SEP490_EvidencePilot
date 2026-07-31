package com.evidencepilot.service;

import com.evidencepilot.dto.response.CitationGraphResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.OpenAlexPreview;
import java.util.UUID;

public interface OpenAlexIngestionService {

    OpenAlexPreview lookupByDoi(String doi);

    default DocumentResponse ingestByDoi(UUID projectId, UUID collectionId, String doi) {
        return ingestByDoi(projectId, collectionId, doi, null);
    }

    DocumentResponse ingestByDoi(
            UUID projectId, UUID collectionId, String doi, UUID categoryId);

    void persistReferences(UUID documentId);

    void persistCitedBy(UUID documentId);

    CitationGraphResponse getCitationGraph(UUID collectionId, boolean includeFailed);
}
