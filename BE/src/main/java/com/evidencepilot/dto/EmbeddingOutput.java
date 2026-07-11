package com.evidencepilot.dto;

import java.util.List;
import java.util.UUID;

public record EmbeddingOutput(UUID jobId, UUID documentId, List<Item> embeddings) {
    public record Item(UUID chunkId, List<Float> embedding) {
    }
}
