package com.evidencepilot.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DatasetGraphResponseDto(
        Integer datasetId,
        List<Node> nodes,
        List<Edge> edges
) {
    public record Node(
            String id,
            String type,
            String label,
            Map<String, Object> data
    ) {
    }

    public record Edge(
            String from,
            String to,
            String type,
            BigDecimal score
    ) {
    }
}
