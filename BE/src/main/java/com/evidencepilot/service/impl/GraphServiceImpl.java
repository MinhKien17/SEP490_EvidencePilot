package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.GraphResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GraphServiceImpl implements GraphService {

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
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

        List<GraphResponse.GraphSectionSummary> sectionSummaries = documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(projectId, DocumentType.PAPER)
                .stream()
                .flatMap(doc -> paperSectionRepository
                        .findByDocumentIdOrderBySectionOrderAsc(doc.getId()).stream())
                .filter(PaperSection::isActive)
                .map(section -> new GraphResponse.GraphSectionSummary(
                        section.getId(),
                        section.getSectionTitle(),
                        0, 0, 0, 0, 0,
                        section.getAssignedUser() != null
                                ? section.getAssignedUser().getId() : null,
                        section.getAssignedUser() != null
                                ? fullName(section.getAssignedUser()) : null))
                .toList();

        return new GraphResponse(List.of(), List.of(), sectionSummaries, 0, false);
    }

    private static String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        return (first + " " + last).trim();
    }
}
