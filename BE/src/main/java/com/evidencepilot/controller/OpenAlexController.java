package com.evidencepilot.controller;

import com.evidencepilot.dto.request.DoiIngestionRequest;
import com.evidencepilot.dto.request.DoiLookupRequest;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.OpenAlexPreview;
import com.evidencepilot.service.OpenAlexIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "OpenAlex DOI Ingestion",
     description = "Lookup and ingest documents via DOI through the OpenAlex API")
public class OpenAlexController {

    private final OpenAlexIngestionService ingestionService;

    @Operation(summary = "Lookup a DOI",
               description = "Fetches metadata from OpenAlex for the given DOI. "
                       + "Returns a preview without persisting anything.")
    @ApiResponse(responseCode = "200", description = "DOI metadata returned")
    @ApiResponse(responseCode = "400", description = "Invalid DOI")
    @PostMapping("/lookup")
    public OpenAlexPreview lookupByDoi(@Valid @RequestBody DoiLookupRequest request) {
        return ingestionService.lookupByDoi(request.doi());
    }

    @Operation(summary = "Ingest by DOI",
               description = "Fetches metadata from OpenAlex, downloads the OA PDF, "
                       + "saves it to MinIO, and triggers the extraction pipeline. "
                       + "Returns 202 Accepted — processing happens asynchronously via RabbitMQ.")
    @ApiResponse(responseCode = "202", description = "Document accepted for processing")
    @ApiResponse(responseCode = "400", description = "DOI not found or no OA PDF available")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @PostMapping("/ingest/doi")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse ingestByDoi(@Valid @RequestBody DoiIngestionRequest request) {
        return ingestionService.ingestByDoi(request.projectId(), request.doi());
    }
}
