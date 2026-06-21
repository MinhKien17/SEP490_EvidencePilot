package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.Paper;
import java.time.LocalDateTime;

public record PaperResponseDto(
        Integer id,
        Integer projectId,
        String fileUrl,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        String extractedText,
        String extractionMethod,
        boolean active,
        LocalDateTime submittedAt
) {
    public static PaperResponseDto fromEntity(Paper paper) {
        if (paper == null) return null;
        return new PaperResponseDto(
                paper.getId(),
                paper.getProject() != null ? paper.getProject().getId() : null,
                paper.getFileUrl(),
                paper.getOriginalFilename(),
                paper.getContentType(),
                paper.getFileSizeBytes(),
                paper.getExtractedText(),
                paper.getExtractionMethod(),
                paper.isActive(),
                paper.getSubmittedAt()
        );
    }
}
