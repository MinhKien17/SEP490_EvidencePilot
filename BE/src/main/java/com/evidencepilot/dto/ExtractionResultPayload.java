package com.evidencepilot.dto;

import java.util.List;
import java.util.UUID;

public record ExtractionResultPayload(
    UUID documentId,
    List<ChunkPayload> chunks
) {
    public record ChunkPayload(
        UUID chunkId,
        Integer chunkIndex,
        String text,
        List<Float> embedding
    ) {}
}
