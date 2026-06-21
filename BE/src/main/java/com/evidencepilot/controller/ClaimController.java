package com.evidencepilot.controller;

import com.evidencepilot.ai.dto.ClaimMatchResponse;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.response.ClaimResponseDto;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.AiAnalysisService;
import com.evidencepilot.service.ClaimMatchingService;
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
    private final ClaimMatchingService claimMatchingService;

    // ── CRUD ───────────────────────────────────────────────────────────────────

    @GetMapping
    public List<ClaimResponseDto> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        List<Claim> claims;
        if (currentUserService.isAdmin(currentUser)) {
            claims = claimRepository.findByActiveTrue();
        } else {
            claims = claimRepository.findByProjectStudentIdAndActiveTrue(currentUser.getId());
        }
        return claims.stream().map(ClaimResponseDto::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ClaimResponseDto findById(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        if (!claim.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id);
        }
        currentUserService.requireClaimAccess(currentUser, claim);
        return ClaimResponseDto.fromEntity(claim);
    }

    @GetMapping("/by-project/{projectId}")
    public List<ClaimResponseDto> findByProject(@PathVariable Integer projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectWriteAccess(currentUser, project);
        return claimRepository.findByProjectIdAndActiveTrue(projectId).stream()
                .map(ClaimResponseDto::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ClaimResponseDto> create(@RequestBody Claim claim) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(ClaimResponseDto.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ClaimResponseDto update(@PathVariable Integer id, @RequestBody Claim claim) {
        User currentUser = currentUserService.requireCurrentUser();
        Claim existing = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        currentUserService.requireProjectWriteAccess(currentUser, existing.getProject());
        claim.setId(id);
        claim.setProject(existing.getProject());
        Claim saved = claimRepository.save(claim);
        return ClaimResponseDto.fromEntity(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Claim existing = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        currentUserService.requireProjectWriteAccess(currentUser, existing.getProject());
        existing.setActive(false);
        claimRepository.save(existing);
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
     * @return the updated Claim with {@code aiConfidenceScore} set and the EvidenceEdge persisted
     */
    @PostMapping("/{id}/analyze")
    public ClaimResponseDto analyze(
            @PathVariable Integer id,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String excerpt,
            @RequestParam(required = false) String title) {

        User currentUser = currentUserService.requireCurrentUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        if (!claim.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id);
        }
        currentUserService.requireProjectWriteAccess(currentUser, claim.getProject());

        boolean hasSourceId = sourceId != null && !sourceId.isBlank();
        boolean hasExcerpt = excerpt != null && !excerpt.isBlank();
        if (hasSourceId != hasExcerpt) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Manual analysis requires both sourceId and excerpt.");
        }
        boolean manualMode = hasSourceId && hasExcerpt;

        Claim result;
        if (manualMode) {
            result = aiAnalysisService.analyzeAndPersist(claim, sourceId, excerpt, title);
        } else {
            result = aiAnalysisService.analyzeAndPersist(claim);
        }
        return ClaimResponseDto.fromEntity(result);
    }

    @GetMapping("/{id}/matches")
    public ClaimMatchResponse matches(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "5") Integer topK) {

        User currentUser = currentUserService.requireCurrentUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
        if (!claim.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id);
        }
        currentUserService.requireClaimAccess(currentUser, claim);
        return claimMatchingService.matchClaim(claim, topK);
    }
}
