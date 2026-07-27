package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaperSectionResponse(
        UUID id,
        UUID documentId,
        UUID assignedUserId,
        Integer sectionOrder,
        String sectionTitle,
        String contentTex,
        String previousContentTex,
        Integer version,
        String contentMdCache,
        LocalDateTime updatedAt) {
}