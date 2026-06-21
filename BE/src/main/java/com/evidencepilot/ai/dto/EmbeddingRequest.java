package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /ai/embeddings}.
 *
 * @param text the text to generate a dense vector embedding for
 */
public record EmbeddingRequest(

        @JsonProperty("text")
        String text
) {}
