package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.TraceabilityExportResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentReference;
import com.evidencepilot.model.EvidenceEdge;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentReferenceRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.EvidenceEdgeRepository;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.ClaimMatchingService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.TraceabilityExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraceabilityExportServiceImpl implements TraceabilityExportService {

    private static final String MISSING = "MISSING";

    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;
    private final DocumentRepository documentRepository;
    private final DocumentReferenceRepository documentReferenceRepository;
    private final FeedbackRequestRepository feedbackRequestRepository;
    private final EvidenceEdgeRepository evidenceEdgeRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ClaimMatchingService claimMatchingService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Override
    public TraceabilityExportResponse exportTraceability(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        if (!project.isActive()) {
            throw new ResourceNotFoundException(projectId, "Project");
        }
        currentUserService.requireProjectAccess(currentUser, project);

        List<DocumentReference> references = documentReferenceRepository
                .findByDocumentProjectIdAndDocumentDocTypeAndDocumentActiveTrueOrderByDocumentIdAscReferenceIndexAsc(
                        projectId, DocumentType.SOURCE);

        Map<UUID, DocumentReference> firstReferenceBySource = references.stream()
                .collect(Collectors.toMap(
                        reference -> reference.getDocument().getId(),
                        reference -> reference,
                        (first, ignored) -> first
                ));
        Map<UUID, Long> referenceCountBySource = references.stream()
                .collect(Collectors.groupingBy(
                        reference -> reference.getDocument().getId(),
                        Collectors.counting()));

        List<TraceabilityExportResponse.TraceabilityClaim> claims = claimRepository
                .findByProjectId(projectId)
                .stream()
                .filter(Claim::isActive)
                .map(claim -> claimExport(claim, projectId, firstReferenceBySource))
                .toList();

        List<TraceabilityExportResponse.TraceabilitySource> sources = documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(projectId, DocumentType.SOURCE)
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
            Map<UUID, DocumentReference> firstReferenceBySource) {

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
                    DocumentReference reference = sourceId != null
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
