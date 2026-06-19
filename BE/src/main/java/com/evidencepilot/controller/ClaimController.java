package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.AiAnalysisService;
import com.evidencepilot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for Claim CRUD operations and AI analysis trigger.
 *
 * <p>Base path: {@code /api/claims}</p>
 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final ProjectRepository projectRepository;
    private final AiAnalysisService aiAnalysisService;
    private final CurrentUserService currentUserService;

    // ── CRUD ───────────────────────────────────────────────────────────────────

    @GetMapping
    public List<Claim> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(currentUser)) {
            return claimRepository.findAll();
        }
        return claimRepository.findByProjectStudentId(currentUser.getId());
    }

    @GetMapping("/{id}")
    public Claim findById(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        currentUserService.requireClaimAccess(currentUser, claim);
        return claim;
    }

    @GetMapping("/by-project/{projectId}")
    public List<Claim> findByProject(@PathVariable Integer projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectAccess(currentUser, project);
        return claimRepository.findByProjectId(projectId);
    }

    @PostMapping
    public ResponseEntity<Claim> create(@RequestBody Claim claim) {
        User currentUser = currentUserService.requireCurrentUser();
        if (claim.getProject() == null || claim.getProject().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project is required.");
        }
        Project project = projectRepository.findById(claim.getProject().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + claim.getProject().getId()));
        currentUserService.requireProjectAccess(currentUser, project);
        claim.setProject(project);
        Claim saved = claimRepository.save(claim);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Claim update(@PathVariable Integer id, @RequestBody Claim claim) {
        User currentUser = currentUserService.requireCurrentUser();
        Claim existing = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        currentUserService.requireClaimAccess(currentUser, existing);
        claim.setId(id);
        claim.setProject(existing.getProject());
        return claimRepository.save(claim);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Claim existing = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        currentUserService.requireClaimAccess(currentUser, existing);
        claimRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── AI analysis ────────────────────────────────────────────────────────────

    /**
     * Triggers the two-phase AI analysis pipeline for a single claim.
     *
     * <h3>Auto mode (no parameters)</h3>
     * <p>The service calls {@code POST /match/claim} first to locate the best
     * source in the AI system, then calls {@code POST /process/claim} with that
     * source.  Use this when you want the AI to pick the evidence automatically.</p>
     *
     * <h3>Manual mode (sourceId + excerpt provided)</h3>
     * <p>Skips the match phase and calls {@code POST /process/claim} directly with
     * the provided {@code sourceId} and {@code excerpt}.  Use this when an instructor
     * wants to pin the analysis to a specific reference.</p>
     *
     * @param id       the Claim to analyse
     * @param sourceId optional – AI-service source identifier to use directly
     * @param excerpt  optional – text excerpt from that source (required when sourceId is set)
     * @param title    optional – source title forwarded to the AI for context
     * @return the updated Claim with {@code aiConfidenceScore} set and the Graph persisted
     */
    @PostMapping("/{id}/analyze")
    public Claim analyze(
            @PathVariable Integer id,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String excerpt,
            @RequestParam(required = false) String title) {

        User currentUser = currentUserService.requireCurrentUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        currentUserService.requireClaimAccess(currentUser, claim);

        boolean manualMode = sourceId != null && !sourceId.isBlank()
                          && excerpt  != null && !excerpt.isBlank();

        if (manualMode) {
            return aiAnalysisService.analyzeAndPersist(claim, sourceId, excerpt, title);
        } else {
            return aiAnalysisService.analyzeAndPersist(claim);
        }
    }
}
