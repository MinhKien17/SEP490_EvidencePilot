package com.evidencepilot.controller;

import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Project;
import com.evidencepilot.repository.ProjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@Tag(name = "Sources", description = "Endpoints for managing source documents")
public class SourceController {

    private final DocumentService documentService;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;

    @Operation(summary = "List sources by project",
            description = "Returns all active source documents belonging to a project.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Source list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/projects/{projectId}")
    public List<DocumentResponse> findByProject(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        currentUserService.requireProjectAccess(currentUserService.requireCurrentUser(),
                projectRepository.findById(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project")));
        // Direct sources (uploaded to project)
        var direct = documentService.getDocumentsByProject(projectId).stream()
                .filter(d -> d.docType() == DocumentType.SOURCE && d.active())
                .toList();
        // Shared sources (from collections via project_document link)
        var shared = projectDocumentRepository.findByProjectId(projectId).stream()
                .map(ProjectDocument::getDocument)
                .filter(d -> d.isActive() && d.getDocType() == DocumentType.SOURCE)
                .map(DocumentResponse::from)
                .toList();
        return Stream.concat(direct.stream(), shared.stream()).distinct().toList();
    }

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

    @Operation(summary = "Remove shared source from project",
            description = "Removes a manual share. A source inherited from a linked collection remains visible "
                    + "but is no longer pinned. Blocked with 409 when the project or its paper sections are not mutable.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Shared source removed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Shared source not found"),
            @ApiResponse(responseCode = "409", description = "Project or paper sections are not mutable")
    })
    @DeleteMapping("/projects/{projectId}/sources/{sourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSharedSource(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId,
            @Parameter(description = "Source document UUID") @PathVariable UUID sourceId) {
        documentService.removeSharedDocument(projectId, sourceId);
    }

    @Operation(summary = "Upload a source file",
            description = "Accepts a file upload (multipart/form-data) and streams it directly to MinIO for processing.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Source uploaded and queued for processing"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameters — at least one of projectId or collectionId is required"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Uploader user or project not found")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Project UUID") @RequestParam(value = "projectId", required = false) UUID projectId,
            @Parameter(description = "Collection UUID") @RequestParam(value = "collectionId", required = false) UUID collectionId) {

        if (projectId == null && collectionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either projectId or collectionId must be provided");
        }
        DocumentResponse response = documentService.uploadDocument(
                projectId, collectionId, file, DocumentType.SOURCE);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
