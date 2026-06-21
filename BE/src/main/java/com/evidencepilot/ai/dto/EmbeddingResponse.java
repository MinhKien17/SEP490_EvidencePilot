package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for {@code POST /ai/embeddings}.
 *
 * <p>The {@code embedding} field contains the dense vector as a list
 * of floating-point values.  Typical dimensionality ranges from 384
 * (nomic-embed-text) to 1536 (text-embedding-3-small).</p>
 *
 * @param embedding the dense vector embedding
 */
public record EmbeddingResponse(

        @JsonProperty("embedding")
        List<Float> embedding
) {}
