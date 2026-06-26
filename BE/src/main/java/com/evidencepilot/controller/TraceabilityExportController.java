package com.evidencepilot.controller;

import com.evidencepilot.dto.response.TraceabilityExportResponse;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.EvidenceEdge;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.SourceReference;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.EvidenceEdgeRepository;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceReferenceRepository;
import com.evidencepilot.repository.SourceRepository;
import com.evidencepilot.service.ClaimMatchingService;
import com.evidencepilot.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Traceability", description = "Project-level traceability matrix export")
public class TraceabilityExportController {

    private static final String MISSING = "MISSING";

    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;
    private final SourceRepository sourceRepository;
    private final SourceReferenceRepository sourceReferenceRepository;
    private final FeedbackRequestRepository feedbackRequestRepository;
    private final EvidenceEdgeRepository evidenceEdgeRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ClaimMatchingService claimMatchingService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Export traceability matrix",
            description = "Generates a full traceability export for a project, including claims, "
                    + "evidence sources, AI matches, evidence edges, and feedback history.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traceability export returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/{projectId}/traceability")
    public TraceabilityExportResponse export(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        if (!project.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }
        currentUserService.requireProjectAccess(currentUser, project);

        List<SourceReference> references = sourceReferenceRepository
                .findBySourceProjectIdAndSourceActiveTrueOrderBySourceIdAscReferenceIndexAsc(projectId);

        Map<UUID, SourceReference> firstReferenceBySource = references.stream()
                .collect(Collectors.toMap(
                        reference -> reference.getSource().getId(),
                        reference -> reference,
                        (first, ignored) -> first
                ));
        Map<UUID, Long> referenceCountBySource = references.stream()
                .collect(Collectors.groupingBy(
                        reference -> reference.getSource().getId(),
                        Collectors.counting()));

        List<TraceabilityExportResponse.TraceabilityClaim> claims = claimRepository
                .findByProjectId(projectId)
                .stream()
                .filter(Claim::isActive)
                .map(claim -> claimExport(claim, projectId, firstReferenceBySource))
                .toList();

        List<TraceabilityExportResponse.TraceabilitySource> sources = sourceRepository
                .findByProjectIdAndActiveTrue(projectId)
                .stream()
                .map(source -> new TraceabilityExportResponse.TraceabilitySource(
                        source.getId(),
                        missingIfBlank(source.getOriginalFilename()),
                        missingIfBlank(source.getContentType()),
                        source.getFileSizeBytes(),
                        missingIfBlank(source.getFileUrl()),
                        referenceCountBySource.getOrDefault(source.getId(), 0L).intValue()))
                .toList();

        List<TraceabilityExportResponse.TraceabilityFeedback> feedback = feedbackRequestRepository
                .findByProjectId(projectId)
                .stream()
                .map(request -> new TraceabilityExportResponse.TraceabilityFeedback(
                        request.getId(),
                        request.getInstructor() == null ? null : request.getInstructor().getId(),
                        request.getStatus()))
                .toList();

        return new TraceabilityExportResponse(
                project.getId(),
                missingIfBlank(project.getTitle()),
                project.getStatus(),
                Instant.now(),
                claims,
                sources,
                feedback);
    }

    private TraceabilityExportResponse.TraceabilityClaim claimExport(
            Claim claim, UUID projectId,
            Map<UUID, SourceReference> firstReferenceBySource) {

        List<TraceabilityExportResponse.TraceabilityMatch> matches = claimMatchingService
                .matchClaim(claim.getId(), projectId)
                .stream()
                .map(suggestion -> {
                    DocumentChunk chunk = suggestion.documentChunkId() != null
                            ? documentChunkRepository.findById(suggestion.documentChunkId()).orElse(null)
                            : null;
                    UUID sourceId = chunk != null && chunk.getDocument() != null
                            ? chunk.getDocument().getId() : null;
                    String filename = chunk != null && chunk.getDocument() != null
                            ? missingIfBlank(chunk.getDocument().getOriginalFilename()) : MISSING;
                    SourceReference reference = sourceId != null
                            ? firstReferenceBySource.get(sourceId) : null;
                    return new TraceabilityExportResponse.TraceabilityMatch(
                            sourceId != null ? sourceId.toString() : MISSING,
                            filename,
                            suggestion.documentChunkId(),
                            null,
                            chunk != null ? missingIfBlank(chunk.getText()) : MISSING,
                            suggestion.score(),
                            suggestion.status(),
                            missingIfBlank(suggestion.explanation()),
                            reference == null ? MISSING : missingIfBlank(reference.getTitle()),
                            reference == null || reference.getPublicationYear() == null
                                    ? MISSING : String.valueOf(reference.getPublicationYear()),
                            reference == null ? MISSING : missingIfBlank(reference.getRawText()));
                })
                .toList();

        List<EvidenceEdge> edges = evidenceEdgeRepository.findByClaimId(claim.getId());
        Map<String, Object> graphData;
        if (edges.isEmpty()) {
            graphData = Map.of("status", MISSING);
        } else {
            EvidenceEdge edge = edges.get(0);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("verdict", edge.getVerdict());
            map.put("confidence", edge.getConfidenceScore());
            map.put("explanation", edge.getExplanation());

            if (edge.getDocumentChunk() != null && edge.getDocumentChunk().getDocument() != null) {
                map.put("matched_source_ids",
                        List.of(String.valueOf(edge.getDocumentChunk().getDocument().getId())));
                map.put("_source_id_used",
                        String.valueOf(edge.getDocumentChunk().getDocument().getId()));
            } else {
                map.put("matched_source_ids", List.of());
                map.put("_source_id_used", "");
            }

            List<String> missingEvidenceList = List.of();
            if (edge.getMissingEvidence() != null && !edge.getMissingEvidence().isBlank()) {
                try {
                    missingEvidenceList = objectMapper.readValue(edge.getMissingEvidence(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                } catch (Exception e) {
                    log.error("Failed to parse missing evidence JSON for edge {}: {}",
                            edge.getId(), edge.getMissingEvidence(), e);
                }
            }
            map.put("missing_evidence", missingEvidenceList);
            graphData = map;
        }

        return new TraceabilityExportResponse.TraceabilityClaim(
                claim.getId(),
                claim.getContent(),
                claim.getAiConfidenceScore(),
                graphData,
                matches);
    }

    private String missingIfBlank(String value) {
        return value == null || value.isBlank() ? MISSING : value;
    }
}
