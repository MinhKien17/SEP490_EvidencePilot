package com.evidencepilot.service;

import com.evidencepilot.dto.ExtractionResultPayload;

public interface QdrantService {
    void upsertVectors(ExtractionResultPayload payload);
}
