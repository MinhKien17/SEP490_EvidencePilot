package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.dto.request.ClaimEvaluationRequest;
import com.evidencepilot.dto.request.ClaimMatchEvaluationRequest;
import com.evidencepilot.dto.request.ClaimUpdateRequest;
import com.evidencepilot.dto.request.MappingReviewRequest;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimMatchCandidateResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.ClaimSourceAuditResponse;
import com.evidencepilot.dto.response.JobSubmitResponse;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Claim CRUD, AI suggestions, and evidence mapping")
public class ClaimController {

    private final ClaimService claimService;

    @Operation(summary = "List all claims",
            description = "Returns all active claims. Admins see everything; "
                    + "other users see claims in their own projects.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public PagedResponse<ClaimResponse> getAllClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active) {
        return claimService.getAllClaims(page, size, sort, q, active);
    }

    @Operation(summary = "Get claim by ID",
            description = "Returns a single claim if the current user has project access.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @GetMapping("/{id}")
    public ClaimResponse getClaimById(
            @Parameter(description = "Claim UUID") @PathVariable UUID id) {
        return claimService.getClaimById(id);
    }

    @Operation(summary = "Create a claim",
            description = "Creates a new claim attached to a paper section and project. "
                    + "Requires write access to the project.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Claim created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse createClaim(@Valid @RequestBody ClaimCreationRequest request) {
        return claimService.createClaim(request);
    }

    @Operation(summary = "Evaluate Claim Quality before creation (async)",
            description = "Validates access synchronously, then queues an AI evaluation job and "
                    + "returns 202 with a jobId. Poll GET /api/jobs/{jobId} for the result.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Evaluation job accepted"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Section not found")
    })
    @PostMapping("/evaluate")
    public ResponseEntity<JobSubmitResponse> evaluateClaim(
            @Valid @RequestBody ClaimEvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(claimService.submitClaimEvaluation(request));
    }

    @Operation(summary = "Update a claim",
            description = "Updates the content and AI confidence score of a claim. "
                    + "Requires write access to the project.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @PutMapping("/{id}")
    public ClaimResponse updateClaim(
            @Parameter(description = "Claim UUID") @PathVariable UUID id,
            @Valid @RequestBody ClaimUpdateRequest request) {
        // DEBT-02: aiConfidenceScore is intentionally not accepted here —
        // it is server-computed only and must never be client-supplied.
        return claimService.updateClaim(id, request.content(), null, request.functionalType());
    }

    @Operation(summary = "Soft-delete a claim",
            description = "Sets the claim's active flag to false. Requires write access to the project.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Claim soft-deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClaim(
            @Parameter(description = "Claim UUID") @PathVariable UUID id) {
        claimService.deleteClaim(id);
    }

    @Operation(summary = "List AI suggestions for a claim",
            description = "Returns all AI-generated evidence suggestions for the specified claim.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suggestion list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @GetMapping("/{id}/suggestions")
    public List<AiSuggestionResponse> getSuggestions(
            @Parameter(description = "Claim UUID") @PathVariable UUID id) {
        return claimService.getSuggestionsForClaim(id);
    }

    @Operation(summary = "Search source matches for a claim",
            description = "Uses the claim embedding and Qdrant to return transient SOURCE chunk candidates. "
                    + "This endpoint does not create suggestions or run generative evaluation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidate list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @PostMapping("/{id}/matches/search")
    public List<ClaimMatchCandidateResponse> searchMatches(
            @Parameter(description = "Claim UUID") @PathVariable UUID id) {
        return claimService.searchMatches(id);
    }

    @Operation(summary = "Evaluate a selected source match (async)",
            description = "Validates access synchronously, then queues an AI evaluation job and "
                    + "returns 202 with a jobId. Poll GET /api/jobs/{jobId} for the result.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Evaluation job accepted"),
            @ApiResponse(responseCode = "400", description = "Chunk is not an eligible project source"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Claim or chunk not found"),
            @ApiResponse(responseCode = "409", description = "Claim changed during evaluation")
    })
    @PostMapping("/{id}/suggestions/evaluate")
    public ResponseEntity<JobSubmitResponse> evaluateMatch(
            @Parameter(description = "Claim UUID") @PathVariable UUID id,
            @Valid @RequestBody ClaimMatchEvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(claimService.submitMatchEvaluation(id, request.documentChunkId()));
    }

    @Operation(summary = "Update suggestion status",
            description = "Accepts or rejects an AI suggestion by setting its status. "
                    + "Replaces the old RPC-style accept/reject endpoints.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Suggestion status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Suggestion not found")
    })
    @PatchMapping("/suggestions/{suggestionId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSuggestionStatus(
            @Parameter(description = "Suggestion UUID") @PathVariable UUID suggestionId,
            @Parameter(description = "New status: ACCEPTED or REJECTED") @RequestParam String status) {
        claimService.updateSuggestionStatus(suggestionId, status);
    }

    @Operation(summary = "List evidence mappings for a claim",
            description = "Returns all evidence mappings (claim-evidence links) for the specified claim.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mapping list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @GetMapping("/{id}/mappings")
    public List<ClaimEvidenceMappingResponse> getMappings(
            @Parameter(description = "Claim UUID") @PathVariable UUID id) {
        return claimService.getMappingsForClaim(id);
    }

    @Operation(summary = "Review a claim-evidence mapping",
            description = "Allows an instructor to verify or reject a student-created evidence mapping. "
                    + "Preserves original AI relation value; stores instructor override separately.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mapping reviewed"),
            @ApiResponse(responseCode = "400", description = "Invalid review data"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    @PatchMapping("/mappings/{mappingId}/review")
    public ClaimEvidenceMappingResponse reviewMapping(
            @Parameter(description = "Mapping UUID") @PathVariable UUID mappingId,
            @Valid @RequestBody MappingReviewRequest request) {
        return claimService.reviewMapping(mappingId, request);
    }

    @Operation(summary = "Audit claim-source connections for a project",
            description = "Returns all claims with their source mappings, highlighting weak or missing connections. "
                    + "Soft-deleted sources appear with sourceActive=false.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit report returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/audit/{projectId}")
    public ClaimSourceAuditResponse auditClaimSources(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        return claimService.auditClaimSources(projectId);
    }
}
