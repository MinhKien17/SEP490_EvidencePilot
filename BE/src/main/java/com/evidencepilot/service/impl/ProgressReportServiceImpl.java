package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.ProgressReportResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.ProgressReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressReportServiceImpl implements ProgressReportService {

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final InstructorFeedbackRepository instructorFeedbackRepository;
    private final CurrentUserService currentUserService;

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

        List<InstructorFeedback> feedbackList = instructorFeedbackRepository.findByRequestProjectId(projectId);
        // Single pass over all feedback items: no per-section re-scan (sections x feedback).
        Map<UUID, int[]> feedbackCounts = new HashMap<>();
        for (InstructorFeedback feedback : feedbackList) {
            if (feedback.getSection() == null) continue;
            int[] counts = feedbackCounts.computeIfAbsent(feedback.getSection().getId(), k -> new int[2]);
            if (feedback.isAnswered()) counts[0]++; else counts[1]++;
        }
        List<ProgressReportResponse.SectionPanel> panels = new ArrayList<>();
        for (PaperSection section : sections) {
            int[] counts = feedbackCounts.getOrDefault(section.getId(), new int[2]);
            panels.add(new ProgressReportResponse.SectionPanel(
                    section.getId(),
                    section.getSectionTitle(),
                    wordCount(section.getContentTex()),
                    section.getAssignedUser() != null ? section.getAssignedUser().getId() : null,
                    section.getAssignedUser() != null ? fullName(section.getAssignedUser()) : null,
                    section.getVersion() != null ? section.getVersion() : 1,
                    section.getUpdatedAt(),
                    counts[0],
                    counts[1]));
        }

        int contentCoverage = sections.isEmpty() ? 0
                : (int) Math.round(sections.stream()
                        .filter(section -> wordCount(section.getContentTex()) > 0)
                        .count() * 100.0 / sections.size());
        List<ProgressReportResponse.ReadinessMetric> metrics = List.of(
                new ProgressReportResponse.ReadinessMetric(
                        "content_coverage", "Content coverage", 100, contentCoverage));

        return new ProgressReportResponse(
                projectId,
                panels,
                new ProgressReportResponse.Readiness(contentCoverage, contentCoverage, metrics));
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
