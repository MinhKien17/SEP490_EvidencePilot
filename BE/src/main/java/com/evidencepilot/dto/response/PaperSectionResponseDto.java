package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.PaperSection;

public record PaperSectionResponseDto(
        Integer id,
        Integer paperId,
        Integer sectionIndex,
        String name,
        String text
) {
    public static PaperSectionResponseDto fromEntity(PaperSection section) {
        if (section == null) return null;
        return new PaperSectionResponseDto(
                section.getId(),
                section.getPaper() != null ? section.getPaper().getId() : null,
                section.getSectionIndex(),
                section.getName(),
                section.getText()
        );
    }
}
