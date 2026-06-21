package com.evidencepilot.dto.response;

import com.evidencepilot.domain.entity.SourceChunk;

public record SourceChunkResponseDto(
        Integer id,
        Integer sourceId,
        Integer chunkIndex,
        Integer page,
        String text,
        String embedding,
        boolean active
) {
    public static SourceChunkResponseDto fromEntity(SourceChunk chunk) {
        if (chunk == null) return null;
        return new SourceChunkResponseDto(
                chunk.getId(),
                chunk.getSource() != null ? chunk.getSource().getId() : null,
                chunk.getChunkIndex(),
                chunk.getPage(),
                chunk.getText(),
                chunk.getEmbedding(),
                chunk.isActive()
        );
    }
}
