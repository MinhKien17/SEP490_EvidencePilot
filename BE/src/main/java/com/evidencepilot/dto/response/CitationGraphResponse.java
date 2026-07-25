package com.evidencepilot.dto.response;

import java.util.List;

public record CitationGraphResponse(
        List<GraphNode> nodes,
        List<GraphEdge> edges
) {
    public record GraphNode(
            String id,
            String doi,
            String title,
            String authors,
            Integer publicationYear,
            boolean inCollection,
            Integer citedByCount,
            boolean hasDoi
    ) {}

    public record GraphEdge(
            String sourceId,
            String targetId,
            String type
    ) {}
}
