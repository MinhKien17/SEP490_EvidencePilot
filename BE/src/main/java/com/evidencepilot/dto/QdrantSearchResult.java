package com.evidencepilot.dto;

import java.math.BigDecimal;

public record QdrantSearchResult(
        String chunkId,
        BigDecimal score
) {
}
