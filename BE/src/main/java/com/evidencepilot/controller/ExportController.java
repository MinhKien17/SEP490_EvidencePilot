package com.evidencepilot.controller;

import com.evidencepilot.model.ExportJob;
import com.evidencepilot.service.ClaimContentConsistencyService;
import com.evidencepilot.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
@Tag(name = "Exports", description = "Async project export (TEX archive)")
public class ExportController {

    private final ExportService exportService;
    private final ClaimContentConsistencyService claimContentConsistencyService;

    @PostMapping
    @Operation(summary = "Start async export", description = "Creates an export job and returns a jobId (202).")
    public ResponseEntity<Map<String, Object>> createExport(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "tex") String format) {
        ExportJob job = exportService.createExportJob(projectId, format);
        return ResponseEntity.accepted()
                .body(Map.of(
                        "jobId", job.getId(),
                        "status", job.getStatus().name(),
                        "warningCount",
                        claimContentConsistencyService.preflight(projectId).warningCount()));
    }

    @GetMapping("/{jobId}/status")
    @Operation(summary = "Get export job status")
    public ExportJob getStatus(@PathVariable UUID jobId) {
        return exportService.getJob(jobId);
    }

    @GetMapping("/{jobId}/download")
    @Operation(summary = "Download export ZIP")
    public ResponseEntity<Resource> download(@PathVariable UUID jobId) {
        Resource content = exportService.downloadExport(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export-" + jobId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @GetMapping
    @Operation(summary = "List user's exports for a project")
    public List<ExportJob> listExports(@RequestParam UUID projectId) {
        return exportService.getUserExports(projectId);
    }
}
