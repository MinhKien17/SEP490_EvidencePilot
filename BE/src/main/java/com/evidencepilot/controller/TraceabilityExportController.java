package com.evidencepilot.controller;

import com.evidencepilot.dto.response.TraceabilityExportResponse;
import com.evidencepilot.service.TraceabilityExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Traceability", description = "Project-level traceability matrix export")
public class TraceabilityExportController {

    private final TraceabilityExportService traceabilityExportService;

    @Operation(summary = "Export traceability matrix",
            description = "Generates a full traceability export for a project, including claims, "
                    + "evidence sources, AI matches, evidence edges, and feedback history.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traceability export returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/{projectId}/traceability")
    public TraceabilityExportResponse export(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        return traceabilityExportService.exportTraceability(projectId);
    }
}
