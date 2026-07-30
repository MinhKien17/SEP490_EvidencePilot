package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.GraphResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.*;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphServiceImpl implements GraphService {

    private static final int MAX_EDGES = 50;

    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;
    private final ClaimEvidenceMappingRepository claimEvidenceMappingRepository;
    private final DocumentRepository documentRepository;
    private final DocumentReferenceRepository documentReferenceRepository;
    private final CurrentUserService currentUserService;

    @Override
    public GraphResponse getGraph(UUID projectId, String scope) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        if (!project.isActive()) {
            throw new ResourceNotFoundException(projectId, "Project");
        }
        currentUserService.requireProjectAccess(currentUser, project);

        List<Claim> claims = claimRepository.findByProjectId(projectId).stream()
                .filter(Claim::isActive)
                .filter(claim -> {
                    if ("all".equalsIgnoreCase(scope)) return true;
                    if (claim.getSection() == null || claim.getSection().getAssignedUser() == null) return false;
                    return claim.getSection().getAssignedUser().getId().equals(currentUser.getId());
                })
                .toList();

        if (claims.isEmpty()) {
            return new GraphResponse(List.of(), List.of(), List.of(), 0, false);
        }

        Map<UUID, Long> referenceCountBySource = documentReferenceRepository
                .findByDocumentProjectIdAndDocumentDocTypeAndDocumentActiveTrueOrderByDocumentIdAscReferenceIndexAsc(
                        projectId, DocumentType.SOURCE)
                .stream()
                .collect(Collectors.groupingBy(
                        ref -> ref.getDocument().getId(),
                        Collectors.counting()));

        Set<UUID> sourceIds = new LinkedHashSet<>();
        List<GraphResponse.GraphClaim> graphClaims = new ArrayList<>();
        List<GraphResponse.GraphEdge> edges = new ArrayList<>();

        for (Claim claim : claims) {
            List<ClaimEvidenceMapping> mappings = claimEvidenceMappingRepository
                    .findByClaimId(claim.getId())
                    .stream()
                    .filter(mapping -> mapping.getStatus() == MappingStatus.ACTIVE)
                    .toList();

            int matchCount = 0;
            Set<String> matchedSourceIds = new LinkedHashSet<>();
            for (ClaimEvidenceMapping mapping : mappings) {
                matchCount++;
                if (mapping.getDocumentChunk() != null && mapping.getDocumentChunk().getDocument() != null) {
                    UUID sourceId = mapping.getDocumentChunk().getDocument().getId();
                    sourceIds.add(sourceId);
                    matchedSourceIds.add(sourceId.toString());

                    EvidenceRelation effectiveRelation = mapping.getRelationOverride() != null
                            ? mapping.getRelationOverride()
                            : mapping.getRelation() != null ? mapping.getRelation() : EvidenceRelation.NEUTRAL;
                    edges.add(new GraphResponse.GraphEdge(
                            claim.getId().toString(),
                            sourceId.toString(),
                            effectiveRelation != null ? effectiveRelation.name() : "NEUTRAL",
                            mapping.getStrengthScore()));
                }
            }

            Map<String, Object> gd = new LinkedHashMap<>();
            if (!mappings.isEmpty()) {
                ClaimEvidenceMapping m = mappings.stream()
                        .max(Comparator
                                .comparingInt((ClaimEvidenceMapping mapping) ->
                                        mapping.getStrengthScore() != null
                                                ? mapping.getStrengthScore()
                                                : -1)
                                .thenComparing(
                                        ClaimEvidenceMapping::getCreatedAt,
                                        Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElseThrow();
                EvidenceRelation rel = m.getRelationOverride() != null ? m.getRelationOverride() : m.getRelation();
                gd.put("verdict", rel != null ? rel.name() : "UNKNOWN");
                gd.put("confidence", m.getStrengthScore());
                gd.put("matched_source_ids", List.copyOf(matchedSourceIds));
                gd.put("missing_evidence", List.of());
            } else {
                gd.put("verdict", "UNKNOWN");
                gd.put("confidence", null);
                gd.put("matched_source_ids", List.of());
                gd.put("missing_evidence", List.of());
            }

            graphClaims.add(new GraphResponse.GraphClaim(
                    claim.getId(),
                    claim.getContent(),
                    claim.getSection() != null ? claim.getSection().getId() : null,
                    claim.getSection() != null ? claim.getSection().getSectionTitle() : null,
                    null,
                    null,
                    gd,
                    matchCount));
        }

        int totalEdges = edges.size();
        boolean hasMore = totalEdges > MAX_EDGES;
        if (edges.size() > MAX_EDGES) {
            edges.sort((a, b) -> {
                int sa = a.score() != null ? a.score() : 0;
                int sb = b.score() != null ? b.score() : 0;
                return Integer.compare(sb, sa);
            });
            edges = edges.subList(0, MAX_EDGES);
        }

        List<GraphResponse.GraphSource> sources = documentRepository
                .findAllById(sourceIds)
                .stream()
                .filter(doc -> doc.getOriginalFilename() != null)
                .map(doc -> new GraphResponse.GraphSource(
                        doc.getId(),
                        doc.getOriginalFilename(),
                        referenceCountBySource.getOrDefault(doc.getId(), 0L).intValue()))
                .toList();

        return new GraphResponse(graphClaims, sources, edges, totalEdges, hasMore);
    }

}
