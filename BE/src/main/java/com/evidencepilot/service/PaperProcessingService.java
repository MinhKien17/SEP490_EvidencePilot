package com.evidencepilot.service;

import com.evidencepilot.dto.response.PaperSectionResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PaperProcessingService {

    List<PaperSectionResponse> getPaperSections(UUID documentId);

    List<PaperSectionResponse> detectAndPersistSections(UUID documentId);

    Map<String, Object> review(UUID documentId, String targetStyle);
}
