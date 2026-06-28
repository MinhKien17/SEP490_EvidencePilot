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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document metadata, chunks, and text retrieval")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Get document by ID",
            description = "Returns metadata for a single document by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/{id}")
    public DocumentResponse getDocumentById(
            @Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.getDocumentById(id);
    }

    @Operation(summary = "Upload a document",
            description = "Uploads a document (multipart/form-data) and streams it directly to MinIO. "
                    + "Returns 202 Accepted — processing happens asynchronously via RabbitMQ.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Document accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse uploadDocument(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Project UUID (optional)") @RequestParam(value = "projectId", required = false) UUID projectId) {
        return documentService.uploadDocument(projectId, file, DocumentType.SOURCE);
    }

    @Operation(summary = "Get document chunks",
            description = "Returns all text chunks for the specified document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chunk list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/{id}/chunks")
    public List<DocumentChunkResponse> getDocumentChunks(
            @Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.getDocumentChunks(id);
    }

    @Operation(summary = "Get document extracted text",
            description = "Returns the full extracted text content for the specified document.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Text returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Document or text not found")
    })
    @GetMapping("/{id}/text")
    public DocumentTextResponse getDocumentText(
            @Parameter(description = "Document UUID") @PathVariable UUID id) {
        return documentService.getDocumentText(id);
    }

    @Operation(summary = "Soft-delete a document",
            description = "Sets the document's active flag to false.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document soft-deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @Parameter(description = "Document UUID") @PathVariable UUID id) {
        documentService.deleteDocument(id);
    }
}
