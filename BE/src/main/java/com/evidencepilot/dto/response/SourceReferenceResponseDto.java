package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.SourceReference;

public record SourceReferenceResponseDto(
        Integer id,
        Integer sourceId,
        Integer referenceIndex,
        String rawText,
        String title,
        Integer year
) {
    public static SourceReferenceResponseDto fromEntity(SourceReference reference) {
        if (reference == null) return null;
        return new SourceReferenceResponseDto(
                reference.getId(),
                reference.getSource() != null ? reference.getSource().getId() : null,
                reference.getReferenceIndex(),
                reference.getRawText(),
                reference.getTitle(),
                reference.getYear()
        );
    }
}
