package com.evidencepilot.controller;

import com.evidencepilot.dto.request.SectionAuditStatusUpdateRequest;
import com.evidencepilot.dto.response.JobSubmitResponse;
import com.evidencepilot.dto.response.SectionAuditFindingResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.SectionAuditFinding;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.SectionAuditFindingStatus;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.SectionAuditFindingRepository;
import com.evidencepilot.service.AiEvaluationService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.impl.SectionAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Section Audits", description = "AI paraphrase-risk and quotation audit of paper sections")
public class SectionAuditController {

    private final SectionAuditService sectionAuditService;
    private final AiEvaluationService aiEvaluationService;
    private final CurrentUserService currentUserService;
    private final PaperSectionRepository paperSectionRepository;
    private final SectionAuditFindingRepository sectionAuditFindingRepository;

    @Operation(summary = "Submit an AI audit job for a paper section",
            description = "Queues an async audit that flags paraphrase-risk and excessive-quotation "
                    + "spans in the saved section content. Poll the job endpoint for the result.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Audit job submitted"),
            @ApiResponse(responseCode = "400", description = "Section has no saved content"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    @PostMapping("/projects/{projectId}/sections/{sectionId}/audit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobSubmitResponse audit(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId,
            @Parameter(description = "Section UUID") @PathVariable UUID sectionId) {
        User currentUser = currentUserService.requireCurrentUser();
        PaperSection section = requireSection(projectId, sectionId);
        currentUserService.requireProjectAccess(currentUser, section.getDocument().getProject());
        if (section.getContentTex() == null || section.getContentTex().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Section audit requires a non-empty saved section");
        }
        return aiEvaluationService.submitSectionAudit(
                projectId,
                sectionId,
                SectionAuditService.fingerprint(section.getContentTex()),
                currentUser.getId());
    }

    @Operation(summary = "List audit findings for a section",
            description = "Returns findings in document order, optionally filtered by status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Findings returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    @GetMapping("/sections/{sectionId}/findings")
    public List<SectionAuditFindingResponse> findings(
            @Parameter(description = "Section UUID") @PathVariable UUID sectionId,
            @Parameter(description = "Optional status filter") @RequestParam(required = false) SectionAuditFindingStatus status) {
        User currentUser = currentUserService.requireCurrentUser();
        PaperSection section = requireSection(sectionId);
        currentUserService.requireProjectAccess(currentUser, section.getDocument().getProject());
        return sectionAuditService.findings(sectionId, status);
    }

    @Operation(summary = "Resolve or dismiss an audit finding",
            description = "Moves a PENDING finding to RESOLVED or DISMISSED. Once moved, it cannot be changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finding status updated"),
            @ApiResponse(responseCode = "400", description = "Status must be RESOLVED or DISMISSED"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Finding not found"),
            @ApiResponse(responseCode = "409", description = "Finding is no longer PENDING")
    })
    @PatchMapping("/sections/findings/{id}/status")
    public SectionAuditFindingResponse updateStatus(
            @Parameter(description = "Finding UUID") @PathVariable UUID id,
            @Valid @RequestBody SectionAuditStatusUpdateRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        SectionAuditFinding finding = sectionAuditFindingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "SectionAuditFinding"));
        currentUserService.requireProjectAccess(
                currentUser, finding.getSection().getDocument().getProject());
        return sectionAuditService.updateStatus(id, request.status(), currentUser.getId());
    }

    private PaperSection requireSection(UUID projectId, UUID sectionId) {
        return paperSectionRepository.findByIdWithDocument(sectionId)
                .filter(PaperSection::isActive)
                .filter(section -> section.getDocument().isActive())
                .filter(section -> section.getDocument().getDocType() == DocumentType.PAPER)
                .filter(section -> section.getDocument().getProject() != null)
                .filter(section -> projectId.equals(section.getDocument().getProject().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
    }

    private PaperSection requireSection(UUID sectionId) {
        return paperSectionRepository.findByIdWithDocument(sectionId)
                .filter(PaperSection::isActive)
                .filter(found -> found.getDocument().isActive())
                .filter(found -> found.getDocument().getDocType() == DocumentType.PAPER)
                .filter(found -> found.getDocument().getProject() != null)
                .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
    }
}
