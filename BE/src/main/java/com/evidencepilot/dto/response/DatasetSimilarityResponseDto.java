package com.evidencepilot.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DatasetSimilarityResponseDto(
        Integer datasetId,
        String query,
        List<Match> matches
) {
    public record Match(
            SourceChunkResponseDto chunk,
            BigDecimal score
    ) {
    }
}
