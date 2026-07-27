package com.evidencepilot.controller;

import com.evidencepilot.dto.response.GraphResponse;
import com.evidencepilot.service.GraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/graph")
@RequiredArgsConstructor
@Tag(name = "Graph", description = "Lightweight claim-evidence graph for the workspace")
public class GraphController {

    private final GraphService graphService;

    @GetMapping
    @Operation(summary = "Get project graph data", description = "Returns claims, sources, and edges for the project graph view")
    public GraphResponse getGraph(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "own") String scope) {
        return graphService.getGraph(projectId, scope);
    }
}
