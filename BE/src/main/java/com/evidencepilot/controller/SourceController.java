package com.evidencepilot.controller;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
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

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentTextRepository documentTextRepository;
    private final CurrentUserService currentUserService;
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
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Source not found: " + id));
        if (!doc.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found: " + id);
        }
        if (doc.getProject() != null) {
            currentUserService.requireProjectAccess(currentUser, doc.getProject());
        }
        return DocumentResponse.from(doc);
    }

    @Operation(summary = "List sources by project",
            description = "Returns active source documents belonging to a project.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sources list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/api/projects/{projectId}/sources")
    public List<DocumentResponse> findByProject(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectAccess(currentUser, project);
        return documentRepository.findByProjectId(projectId).stream()
                .filter(Document::isActive)
                .map(DocumentResponse::from)
                .toList();
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
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Source not found: " + id));
        if (!doc.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found: " + id);
        }
        if (doc.getProject() != null) {
            currentUserService.requireProjectAccess(currentUser, doc.getProject());
        }
        return documentChunkRepository.findByDocumentId(id).stream()
                .map(chunk -> new DocumentChunkResponse(
                        chunk.getId(), chunk.getDocument().getId(),
                        chunk.getChunkIndex(), chunk.getText(), chunk.isActive()))
                .toList();
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
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Source not found: " + id));
        if (!doc.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found: " + id);
        }
        if (doc.getProject() != null) {
            currentUserService.requireProjectAccess(currentUser, doc.getProject());
        }
        DocumentText dt = documentTextRepository.findByDocumentId(id);
        if (dt == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Extracted text not found");
        }
        return new DocumentTextResponse(dt.getId(), dt.getDocument().getId(),
                dt.getExtractedText(), dt.getExtractionMethod());
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
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Source not found: " + id));
        if (doc.getProject() != null) {
            currentUserService.requireProjectWriteAccess(currentUser, doc.getProject());
        }
        doc.setActive(false);
        documentRepository.save(doc);
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
    @Transactional
    public ResponseEntity<DocumentResponse> upload(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "UUID of the uploader user") @RequestParam("uploadedBy") UUID uploadedById,
            @Parameter(description = "Project UUID (optional for project-scoped sources)") @RequestParam(value = "projectId", required = false) UUID projectId) {

        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireUserIdOrAdmin(currentUser, uploadedById);

        userRepository.findById(uploadedById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + uploadedById));

        DocumentResponse response = documentService.uploadDocument(projectId, file, DocumentType.EVIDENCE_SOURCE);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
