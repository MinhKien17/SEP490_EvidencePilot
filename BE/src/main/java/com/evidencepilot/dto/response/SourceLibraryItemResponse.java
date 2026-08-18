package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.ProcessingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SourceLibraryItemResponse(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        ProcessingStatus processingStatus,
        String processingError,
        LocalDateTime createdAt,
        List<Usage> collections,
        List<Usage> projects
) {
    public record Usage(UUID id, String name) {}
}
