package com.evidencepilot.controller;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@Tag(name = "Sources", description = "Endpoints for managing source documents")
public class SourceController {

    private final DocumentService documentService;

    @Operation(summary = "Get source by ID", description = "Returns metadata for a single active source document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Source metadata returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Source not found or inactive")
    })
    @GetMapping("/{id}")
    public DocumentResponse findById(
            @Parameter(description = "Source document UUID") @PathVariable UUID id) {
        return documentService.getSourceById(id);
    }

    @Operation(summary = "Get text chunks of a source",
            description = "Retrieves all active text chunks for the specified source document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunks list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Source not found")
    })
    @GetMapping("/{id}/chunks")
    public List<DocumentChunkResponse> chunks(
            @Parameter(description = "Source document UUID") @PathVariable UUID id) {
        return documentService.getDocumentChunks(id);
    }

    @Operation(summary = "Get extracted text of a source",
            description = "Returns the full extracted text content for the specified source document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Text returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Source or extracted text not found")
    })
    @GetMapping("/{id}/text")
    public DocumentTextResponse text(
            @Parameter(description = "Source document UUID") @PathVariable UUID id) {
        return documentService.getDocumentText(id);
    }

    @Operation(summary = "Soft-delete source by ID",
            description = "Soft-deletes a source document by setting active=false.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Source soft-deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Source not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Source document UUID") @PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload a source file",
            description = "Accepts a file upload (multipart/form-data) and streams it directly to MinIO for processing.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Source uploaded and queued for processing"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Uploader user or project not found")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Project UUID (optional for project-scoped sources)") @RequestParam(value = "projectId", required = false) UUID projectId,
            @Parameter(description = "Collection UUID (optional for collection-scoped sources)") @RequestParam(value = "collectionId", required = false) UUID collectionId,
            @Parameter(description = "Source category UUID (optional)") @RequestParam(value = "sourceCategoryId", required = false) String sourceCategoryId) {

        DocumentResponse response = documentService.uploadDocument(
                projectId, collectionId, parseOptionalUuid(sourceCategoryId), file, DocumentType.SOURCE);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID parseOptionalUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sourceCategoryId");
        }
    }
}
