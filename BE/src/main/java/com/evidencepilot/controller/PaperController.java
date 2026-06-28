package com.evidencepilot.controller;

import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.PaperProcessingService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Tag(name = "Papers", description = "Student paper submissions, sections, and AI review")
public class PaperController {

    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final PaperProcessingService paperProcessingService;
    private final DocumentService documentService;

    @Operation(summary = "List all papers",
            description = "Returns all active paper documents. "
                    + "Admins see all; students see only their own papers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paper list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<DocumentResponse> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        List<Document> docs;
        if (currentUserService.isAdmin(currentUser)) {
            docs = documentRepository.findAll().stream()
                    .filter(this::isActivePaper)
                    .toList();
        } else {
            docs = documentRepository.findAll().stream()
                    .filter(d -> isActivePaper(d) && d.getProject() != null
                            && d.getProject().getStudent() != null
                            && d.getProject().getStudent().getId().equals(currentUser.getId()))
                    .toList();
        }
        return docs.stream().map(DocumentResponse::from).toList();
    }

    @Operation(summary = "Get paper by ID",
            description = "Returns a single paper document by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paper returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @GetMapping("/{id}")
    public DocumentResponse findById(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = requirePaperDocument(id);
        if (doc.getProject() != null) {
            currentUserService.requireProjectWriteAccess(currentUser, doc.getProject());
        }
        return DocumentResponse.from(doc);
    }

    @Operation(summary = "List papers by project",
            description = "Returns all active paper documents belonging to a project.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paper list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/api/projects/{projectId}/papers")
    public List<DocumentResponse> findByProject(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectWriteAccess(currentUser, project);
        return documentRepository.findByProjectId(projectId).stream()
                .filter(this::isActivePaper)
                .map(DocumentResponse::from)
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
    @GetMapping("/{id}/sections")
    public List<PaperSectionResponse> sections(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = requirePaperDocument(id);
        currentUserService.requireProjectAccess(currentUser, doc.getProject());
        return paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(id).stream()
                .map(s -> new PaperSectionResponse(
                        s.getId(), s.getDocument().getId(),
                        s.getAssignedUser() != null ? s.getAssignedUser().getId() : null,
                        s.getSectionOrder(), s.getSectionTitle(),
                        s.getContentTex(), s.getContentMdCache(), s.getUpdatedAt()))
                .toList();
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
    @PostMapping("/{id}/reviews")
    public Map<String, Object> review(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id,
            @Parameter(description = "Target output style (optional)") @RequestParam(required = false) String targetStyle) {

        User currentUser = currentUserService.requireCurrentUser();
        Document doc = requirePaperDocument(id);
        currentUserService.requireProjectAccess(currentUser, doc.getProject());
        return paperProcessingService.review(doc, targetStyle);
    }

    @Operation(summary = "Soft-delete a paper",
            description = "Sets the paper's active flag to false. Requires project access.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paper soft-deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Paper not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Paper document UUID") @PathVariable UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = requirePaperDocument(id);
        currentUserService.requireProjectAccess(currentUser, doc.getProject());
        doc.setActive(false);
        documentRepository.save(doc);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload a paper",
            description = "Uploads a student paper (multipart/form-data) and queues it for "
                    + "section detection and processing. The projectId is extracted from the JWT context.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paper uploaded and sections detected"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<DocumentResponse> upload(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Project UUID") @RequestParam("projectId") UUID projectId) {

        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectAccess(currentUser, project);

        DocumentResponse response = documentService.uploadDocument(projectId, file, DocumentType.PAPER);

        Document saved = documentRepository.findById(response.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Saved document not found immediately after upload"));
        paperProcessingService.detectAndPersistSections(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Document requirePaperDocument(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        if (!isActivePaper(doc)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        return doc;
    }

    private boolean isActivePaper(Document doc) {
        return doc.isActive() && doc.getDocType() == DocumentType.PAPER;
    }
}
