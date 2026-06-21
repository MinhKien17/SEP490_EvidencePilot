package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.Source;

public record SourceResponseDto(
        Integer id,
        String fileUrl,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        boolean active,
        Integer projectId,
        Integer datasetId,
        Integer uploadedById
) {
    public static SourceResponseDto fromEntity(Source source) {
        if (source == null) return null;
        return new SourceResponseDto(
                source.getId(),
                source.getFileUrl(),
                source.getOriginalFilename(),
                source.getContentType(),
                source.getFileSizeBytes(),
                source.isActive(),
                source.getProject() != null ? source.getProject().getId() : null,
                source.getDataset() != null ? source.getDataset().getId() : null,
                source.getUploadedBy() != null ? source.getUploadedBy().getId() : null
        );
    }
}
