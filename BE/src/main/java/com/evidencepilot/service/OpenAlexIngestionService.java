package com.evidencepilot.service;

import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.OpenAlexPreview;
import java.util.UUID;

public interface OpenAlexIngestionService {

    OpenAlexPreview lookupByDoi(String doi);

    DocumentResponse ingestByDoi(UUID projectId, String doi);
}
