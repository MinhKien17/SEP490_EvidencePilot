package com.evidencepilot.dto;

import java.util.List;
import java.util.UUID;

public record EmbeddingManifest(UUID jobId, UUID documentId, List<Chunk> chunks) {
    public record Chunk(UUID chunkId, int chunkIndex, String text) {
    }
}
