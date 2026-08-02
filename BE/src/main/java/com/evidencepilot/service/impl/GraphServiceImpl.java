package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.GraphResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.FunctionalType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentReferenceRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.ClaimContentConsistencyService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.EvidenceFilterService;
import com.evidencepilot.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GraphServiceImpl implements GraphService {

    private static final int MAX_EDGES = 150;

    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;
    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final DocumentReferenceRepository documentReferenceRepository;
    private final CurrentUserService currentUserService;
    private final EvidenceFilterService evidenceFilterService;
    private final ClaimContentConsistencyService claimContentConsistencyService;

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
            return new GraphResponse(
                    List.of(), List.of(), List.of(),
                    buildSectionSummaries(projectId, Map.of()), 0, false);
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
        Map<UUID, SectionTally> tallyBySection = new LinkedHashMap<>();

        for (Claim claim : claims) {
            List<ClaimEvidenceMapping> mappings = evidenceFilterService.activeMappings(claim);

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

            ClaimContentStatus contentStatus = claimContentConsistencyService.evaluate(claim);
            graphClaims.add(new GraphResponse.GraphClaim(
                    claim.getId(),
                    claim.getContent(),
                    claim.getSection() != null ? claim.getSection().getId() : null,
                    claim.getSection() != null ? claim.getSection().getSectionTitle() : null,
                    claim.getCreatedBy() != null ? claim.getCreatedBy().getId() : null,
                    claim.getCreatedBy() != null ? fullName(claim.getCreatedBy()) : null,
                    contentStatus,
                    gd,
                    matchCount));

            UUID sectionId = claim.getSection() != null ? claim.getSection().getId() : null;
            SectionTally tally = tallyBySection.computeIfAbsent(sectionId, id -> new SectionTally());
            tally.claims++;
            switch (contentStatus) {
                case PRESENT -> tally.present++;
                case MISSING -> tally.missing++;
                case ORPHANED -> tally.orphaned++;
            }
            if (matchCount == 0) {
                tally.unsupported++;
            }
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
                        referenceCountBySource.getOrDefault(doc.getId(), 0L).intValue(),
                        doc.getOpenAlexTopic()))
                .toList();

        List<GraphResponse.GraphSectionSummary> sectionSummaries =
                buildSectionSummaries(projectId, tallyBySection);

        return new GraphResponse(
                graphClaims, sources, edges, sectionSummaries, totalEdges, hasMore);
    }

    @Override
    public GraphResponse.ClaimStatsResponse getClaimStats(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        if (!project.isActive()) {
            throw new ResourceNotFoundException(projectId, "Project");
        }
        currentUserService.requireProjectAccess(currentUser, project);

        Map<String, Long> byFunctionalType = new LinkedHashMap<>();
        for (FunctionalType type : FunctionalType.values()) {
            byFunctionalType.put(type.name(), 0L);
        }
        long total = 0;
        for (ClaimRepository.FunctionalTypeCount row : claimRepository.countByFunctionalType(projectId)) {
            if (row.getFunctionalType() != null) {
                byFunctionalType.put(row.getFunctionalType().name(), row.getClaimCount());
            }
            total += row.getClaimCount();
        }
        return new GraphResponse.ClaimStatsResponse((int) total, byFunctionalType);
    }

    // ponytail: section radar/coverage must reflect the paper's full structure, not just
    // sections that happen to have claims; claims tallied by section id
    private List<GraphResponse.GraphSectionSummary> buildSectionSummaries(
            UUID projectId, Map<UUID, SectionTally> tallyBySection) {
        return documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(projectId, DocumentType.PAPER)
                .stream()
                .flatMap(doc -> paperSectionRepository
                        .findByDocumentIdOrderBySectionOrderAsc(doc.getId()).stream())
                .filter(PaperSection::isActive)
                .map(section -> {
                    SectionTally tally = tallyBySection.get(section.getId());
                    return new GraphResponse.GraphSectionSummary(
                            section.getId(),
                            section.getSectionTitle(),
                            tally != null ? tally.claims : 0,
                            tally != null ? tally.present : 0,
                            tally != null ? tally.missing : 0,
                            tally != null ? tally.orphaned : 0,
                            tally != null ? tally.unsupported : 0,
                            section.getAssignedUser() != null
                                    ? section.getAssignedUser().getId() : null,
                            section.getAssignedUser() != null
                                    ? fullName(section.getAssignedUser()) : null);
                })
                .toList();
    }

    private static final class SectionTally {
        int claims;
        int present;
        int missing;
        int orphaned;
        int unsupported;
    }

    private static String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        return (first + " " + last).trim();
    }

}
