package com.evidencepilot.controller;

import com.evidencepilot.ai.dto.ClaimMatch;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceReference;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.TraceabilityExportResponse;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.GraphRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceReferenceRepository;
import com.evidencepilot.repository.SourceRepository;
import com.evidencepilot.service.ClaimMatchingService;
import com.evidencepilot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class TraceabilityExportController {

    private static final String MISSING = "MISSING";

    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;
    private final SourceRepository sourceRepository;
    private final SourceReferenceRepository sourceReferenceRepository;
    private final FeedbackRequestRepository feedbackRequestRepository;
    private final GraphRepository graphRepository;
    private final ClaimMatchingService claimMatchingService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{projectId}/traceability-export")
    public TraceabilityExportResponse export(@PathVariable Integer projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        if (!project.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }
        currentUserService.requireProjectAccess(currentUser, project);

        List<SourceReference> references = sourceReferenceRepository
                .findBySourceProjectIdAndSourceActiveTrueOrderBySourceIdAscReferenceIndexAsc(projectId)
                .stream()
                .toList();
        Map<Integer, SourceReference> firstReferenceBySource = references.stream()
                .collect(Collectors.toMap(
                        reference -> reference.getSource().getId(),
                        reference -> reference,
                        (first, ignored) -> first
                ));
        Map<Integer, Long> referenceCountBySource = references.stream()
                .collect(Collectors.groupingBy(reference -> reference.getSource().getId(), Collectors.counting()));

        List<TraceabilityExportResponse.TraceabilityClaim> claims = claimRepository
                .findByProjectIdAndActiveTrue(projectId)
                .stream()
                .map(claim -> claimExport(claim, firstReferenceBySource))
                .toList();

        List<TraceabilityExportResponse.TraceabilitySource> sources = sourceRepository
                .findByProjectIdAndActiveTrue(projectId)
                .stream()
                .map(source -> sourceExport(source, referenceCountBySource))
                .toList();

        List<TraceabilityExportResponse.TraceabilityFeedback> feedback = feedbackRequestRepository
                .findByProjectId(projectId)
                .stream()
                .map(request -> new TraceabilityExportResponse.TraceabilityFeedback(
                        request.getId(),
                        request.getInstructor() == null ? null : request.getInstructor().getId(),
                        request.getStatus()
                ))
                .toList();

        return new TraceabilityExportResponse(
                project.getId(),
                missingIfBlank(project.getTitle()),
                project.getStatus(),
                Instant.now(),
                claims,
                sources,
                feedback
        );
    }

    private TraceabilityExportResponse.TraceabilityClaim claimExport(
            Claim claim,
            Map<Integer, SourceReference> firstReferenceBySource) {

        List<TraceabilityExportResponse.TraceabilityMatch> matches = claimMatchingService
                .matchClaim(claim, 5)
                .matches()
                .stream()
                .map(match -> matchExport(match, firstReferenceBySource))
                .toList();

        Map<String, Object> graphData = graphRepository.findByClaimId(claim.getId())
                .map(graph -> graph.getGraphData())
                .orElse(Map.of("status", MISSING));

        return new TraceabilityExportResponse.TraceabilityClaim(
                claim.getId(),
                claim.getContent(),
                claim.getAiConfidenceScore(),
                graphData,
                matches
        );
    }

    private TraceabilityExportResponse.TraceabilityMatch matchExport(
            ClaimMatch match,
            Map<Integer, SourceReference> firstReferenceBySource) {

        Integer sourceId = parseInteger(match.sourceId());
        SourceReference reference = sourceId == null ? null : firstReferenceBySource.get(sourceId);
        return new TraceabilityExportResponse.TraceabilityMatch(
                match.sourceId(),
                missingIfBlank(match.filename()),
                match.chunkId(),
                match.page(),
                match.excerpt(),
                match.score(),
                match.suitability(),
                match.explanation(),
                reference == null ? MISSING : missingIfBlank(reference.getTitle()),
                reference == null || reference.getYear() == null ? MISSING : String.valueOf(reference.getYear()),
                reference == null ? MISSING : missingIfBlank(reference.getRawText())
        );
    }

    private TraceabilityExportResponse.TraceabilitySource sourceExport(
            Source source,
            Map<Integer, Long> referenceCountBySource) {

        int referenceCount = referenceCountBySource.getOrDefault(source.getId(), 0L).intValue();
        return new TraceabilityExportResponse.TraceabilitySource(
                source.getId(),
                missingIfBlank(source.getOriginalFilename()),
                missingIfBlank(source.getContentType()),
                source.getFileSizeBytes(),
                missingIfBlank(source.getFileUrl()),
                referenceCount
        );
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String missingIfBlank(String value) {
        return value == null || value.isBlank() ? MISSING : value;
    }
}
