package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.ProgressReportResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.ClaimContentConsistencyService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.EvidenceFilterService;
import com.evidencepilot.service.ProgressReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressReportServiceImpl implements ProgressReportService {

    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;
    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final InstructorFeedbackRepository instructorFeedbackRepository;
    private final CurrentUserService currentUserService;
    private final EvidenceFilterService evidenceFilterService;
    private final ClaimContentConsistencyService claimContentConsistencyService;

    @Override
    public ProgressReportResponse getProgressReport(UUID projectId, String memberFilter) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        if (!project.isActive()) {
            throw new ResourceNotFoundException(projectId, "Project");
        }
        currentUserService.requireProjectAccess(currentUser, project);

        List<PaperSection> sections = new ArrayList<>();
        for (Document paper : documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(projectId, DocumentType.PAPER)) {
            sections.addAll(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(paper.getId())
                    .stream().filter(PaperSection::isActive).toList());
        }
        UUID filterUserId = memberFilter == null || memberFilter.isBlank()
                || "ALL".equalsIgnoreCase(memberFilter) ? null : parseUuid(memberFilter);
        if (filterUserId != null) {
            sections.removeIf(section -> section.getAssignedUser() == null
                    || !section.getAssignedUser().getId().equals(filterUserId));
        }
        Map<UUID, PaperSection> sectionById = new LinkedHashMap<>();
        for (PaperSection section : sections) {
            sectionById.put(section.getId(), section);
        }

        List<InstructorFeedback> feedbackList = instructorFeedbackRepository.findByRequestProjectId(projectId);
        List<Claim> claims = claimRepository.findByProjectId(projectId).stream()
                .filter(Claim::isActive)
                .filter(claim -> claim.getSection() == null || sectionById.containsKey(claim.getSection().getId()))
                .toList();

        List<ProgressReportResponse.SectionPanel> panels = new ArrayList<>();
        List<ProgressReportResponse.MatrixRow> matrix = new ArrayList<>();
        int claimsPresent = 0;
        int claimsWithEvidence = 0;

        for (Claim claim : claims) {
            List<ClaimEvidenceMapping> mappings = evidenceFilterService.activeMappings(claim);
            ClaimContentStatus status = claimContentConsistencyService.evaluate(claim);
            if (status == ClaimContentStatus.PRESENT) claimsPresent++;
            if (!mappings.isEmpty()) claimsWithEvidence++;

            ClaimEvidenceMapping strongest = mappings.stream()
                    .max(Comparator.comparingInt((ClaimEvidenceMapping m) ->
                            m.getStrengthScore() != null ? m.getStrengthScore() : -1)
                            .thenComparing(ClaimEvidenceMapping::getCreatedAt,
                                    Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);
            EvidenceRelation relation = strongest == null ? null
                    : strongest.getRelationOverride() != null ? strongest.getRelationOverride()
                    : strongest.getRelation();

            matrix.add(new ProgressReportResponse.MatrixRow(
                    claim.getId(),
                    claim.getContent(),
                    claim.getSection() != null ? claim.getSection().getId() : null,
                    claim.getSection() != null ? claim.getSection().getSectionTitle() : null,
                    status,
                    mappings.size(),
                    relation != null ? relation.name() : null,
                    strongest != null ? strongest.getStrengthScore() : null,
                    claim.getCreatedBy() != null ? claim.getCreatedBy().getId() : null,
                    claim.getCreatedBy() != null ? fullName(claim.getCreatedBy()) : null,
                    claim.getUpdatedAt()));
        }

        int totalClaimCount = claims.size();
        for (PaperSection section : sections) {
            long claimCount = claims.stream()
                    .filter(claim -> claim.getSection() != null
                            && claim.getSection().getId().equals(section.getId()))
                    .count();
            int answered = 0;
            int unanswered = 0;
            for (InstructorFeedback feedback : feedbackList) {
                if (feedback.getSection() != null && section.getId().equals(feedback.getSection().getId())) {
                    if (feedback.isAnswered()) answered++; else unanswered++;
                }
            }
            panels.add(new ProgressReportResponse.SectionPanel(
                    section.getId(),
                    section.getSectionTitle(),
                    wordCount(section.getContentTex()),
                    (int) claimCount,
                    section.getAssignedUser() != null ? section.getAssignedUser().getId() : null,
                    section.getAssignedUser() != null ? fullName(section.getAssignedUser()) : null,
                    section.getVersion() != null ? section.getVersion() : 1,
                    section.getUpdatedAt(),
                    answered,
                    unanswered));
        }

        int contentCoverage = sections.isEmpty() ? 0
                : (int) Math.round(sections.stream()
                        .filter(section -> wordCount(section.getContentTex()) > 0)
                        .count() * 100.0 / sections.size());
        int claimsPresentPercent = totalClaimCount == 0 ? 0
                : (int) Math.round(claimsPresent * 100.0 / totalClaimCount);
        int claimsWithEvidencePercent = totalClaimCount == 0 ? 0
                : (int) Math.round(claimsWithEvidence * 100.0 / totalClaimCount);
        int score = (contentCoverage + claimsPresentPercent + claimsWithEvidencePercent) / 3;

        return new ProgressReportResponse(
                projectId,
                panels,
                matrix,
                new ProgressReportResponse.Readiness(
                        score, contentCoverage, claimsPresentPercent, claimsWithEvidencePercent));
    }

    private static int wordCount(String contentTex) {
        if (contentTex == null || contentTex.isBlank()) return 0;
        return contentTex.trim().split("\\s+").length;
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        return (first + " " + last).trim();
    }
}
