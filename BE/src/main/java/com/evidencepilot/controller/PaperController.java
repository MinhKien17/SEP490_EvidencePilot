package com.evidencepilot.controller;

import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.PaperValidationResponse;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.PaperProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Papers", description = "Student paper submissions, sections, and AI review")
public class PaperController {

    private final DocumentService documentService;
    private final PaperProcessingService paperProcessingService;

    @Operation(summary = "List all papers",
            description = "Returns all active paper documents. "
                    + "Admins see all; students see only their own papers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paper list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/papers")
    public List<DocumentResponse> findAll() {
        return documentService.getAllPapersForCurrentUser();
    }

    @Operation(summary = "Get paper by ID",
            description = "Returns a single paper document by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paper returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @GetMapping("/papers/{id}")
    public DocumentResponse findById(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id) {
        DocumentResponse doc = documentService.getDocumentById(id);
        if (doc.docType() != DocumentType.PAPER || !doc.active()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        return doc;
    }

    @Operation(summary = "List papers by project",
            description = "Returns all active paper documents belonging to a project.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paper list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/projects/{projectId}/papers")
    public List<DocumentResponse> findByProject(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        return documentService.getDocumentsByProject(projectId).stream()
                .filter(d -> d.docType() == DocumentType.PAPER && d.active())
                .toList();
    }

    @Operation(summary = "Get paper sections",
            description = "Returns all sections of a paper document in order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Section list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @GetMapping("/papers/{id}/sections")
    public List<PaperSectionResponse> sections(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id) {
        return paperProcessingService.getPaperSections(id);
    }

    @Operation(summary = "Validate paper against standard",
            description = "Compares detected paper sections against the project's targetStandard. "
                    + "Returns missing, extra, and out-of-order sections.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @GetMapping("/papers/{id}/validate")
    public PaperValidationResponse validate(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id) {
        return paperProcessingService.validateSections(id);
    }

    @Operation(summary = "Update a paper section",
            description = "Rename, reorder, or merge a paper section. "
                    + "Set mergeIntoId to merge this section into another.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Section updated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    @PutMapping("/papers/{documentId}/sections/{sectionId}")
    public PaperSectionResponse updateSection(
            @Parameter(description = "Paper document UUID") @PathVariable UUID documentId,
            @Parameter(description = "Section UUID") @PathVariable UUID sectionId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer order,
            @RequestParam(required = false) UUID mergeIntoId) {
        return paperProcessingService.updateSection(documentId, sectionId, title, order, mergeIntoId);
    }

    @Operation(summary = "Create a new paper section",
            description = "Adds a new section to a paper document. "
                    + "Optionally specify a parent section ID for ordering.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Section created"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @PostMapping("/papers/{documentId}/sections/create")
    @ResponseStatus(HttpStatus.CREATED)
    public PaperSectionResponse createSection(
            @Parameter(description = "Paper document UUID") @PathVariable UUID documentId,
            @RequestParam String title,
            @RequestParam(required = false) UUID parentSectionId) {
        return paperProcessingService.createSection(documentId, title, parentSectionId);
    }

    @Operation(summary = "Generate AI paper review",
            description = "Runs AI review against the paper and returns suggestions. "
                    + "Optional targetStyle parameter controls the output format style.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review generated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @PostMapping("/papers/{id}/reviews")
    public Map<String, Object> review(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id,
            @Parameter(description = "Target output style (optional)") @RequestParam(required = false) String targetStyle) {
        return paperProcessingService.review(id, targetStyle);
    }

    @Operation(summary = "Soft-delete a paper",
            description = "Sets the paper's active flag to false. Requires project access.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paper soft-deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @DeleteMapping("/papers/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload a paper",
            description = "Uploads a student paper (multipart/form-data) and queues it for "
                    + "section detection and processing. The projectId is extracted from the JWT context.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paper uploaded and queued for processing"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @PostMapping(value = "/papers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<DocumentResponse> upload(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Project UUID") @RequestParam("projectId") UUID projectId) {

        DocumentResponse response = documentService.uploadDocument(projectId, file, DocumentType.PAPER);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
