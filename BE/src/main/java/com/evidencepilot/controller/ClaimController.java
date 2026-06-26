package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.EvidenceEdgeResponse;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import java.util.Map;
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
    public List<ClaimResponse> getAllClaims() {
        return claimService.getAllClaims();
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

    @Operation(summary = "List claims by project",
            description = "Returns all active claims belonging to a project.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/api/projects/{projectId}/claims")
    public List<ClaimResponse> getClaimsByProject(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        return claimService.getClaimsByProject(projectId);
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
            @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Float aiConfidenceScore = body.get("aiConfidenceScore") != null
                ? ((Number) body.get("aiConfidenceScore")).floatValue()
                : null;
        return claimService.updateClaim(id, content, aiConfidenceScore);
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

    @Operation(summary = "Create an AI suggestion",
            description = "Creates a new AI suggestion linking a document chunk to a claim. "
                    + "The suggestion starts in PENDING status.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Suggestion created"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @PostMapping("/{id}/suggestions")
    @ResponseStatus(HttpStatus.CREATED)
    public AiSuggestionResponse createSuggestion(
            @Parameter(description = "Claim UUID") @PathVariable UUID id,
            @Parameter(description = "Document chunk UUID") @RequestParam UUID documentChunkId,
            @Parameter(description = "Similarity score") @RequestParam Float score,
            @Parameter(description = "Explanation of the match") @RequestParam String explanation) {
        return claimService.createSuggestion(id, documentChunkId, score, explanation);
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
            @Parameter(description = "New status: ACCEPTED or REJECTED") @RequestParam SuggestionStatus status) {
        if (status == SuggestionStatus.ACCEPTED) {
            claimService.acceptSuggestion(suggestionId);
        } else if (status == SuggestionStatus.REJECTED) {
            claimService.rejectSuggestion(suggestionId);
        }
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

    @Operation(summary = "List evidence edges for a claim",
            description = "Returns all evidence edges (AI verdict graph) for the specified claim.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Edge list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @GetMapping("/{id}/edges")
    public List<EvidenceEdgeResponse> getEdges(
            @Parameter(description = "Claim UUID") @PathVariable UUID id) {
        return claimService.getEdgesForClaim(id);
    }
}
