package com.evidencepilot.service;

import java.math.BigDecimal;

public record QdrantSearchResult(
        String chunkId,
        BigDecimal score
) {
}
