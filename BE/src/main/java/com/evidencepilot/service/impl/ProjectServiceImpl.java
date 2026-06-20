package com.evidencepilot.service.impl;

import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.enums.ProjectStatus;
import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link ProjectService}.
 *
 * <p>
 * Every read and write operation is scoped to the authenticated student via
 * the tenant-safe {@link ProjectRepository} finders. If the repository returns
 * empty, a {@link ResourceNotFoundException} is thrown — this deliberately
 * covers both "does not exist" and "belongs to another tenant" to prevent
 * resource enumeration.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects(Integer authenticatedStudentId) {
        return projectRepository.findAllByStudentIdAndActiveTrue(authenticatedStudentId)
                .stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Integer id, Integer authenticatedStudentId) {
        Project project = projectRepository
                .findByIdAndStudentIdAndActiveTrue(id, authenticatedStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return ProjectResponse.fromEntity(project);
    }

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, Integer authenticatedStudentId) {
        Project project = new Project();
        project.setStudent(userRepository.getReferenceById(authenticatedStudentId));
        project.setTitle(request.title());
        project.setDescription(request.description());
        // status and active use entity defaults: DRAFT and true

        Project saved = projectRepository.save(project);
        return ProjectResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Integer id, ProjectUpdateRequest request,
                                         Integer authenticatedStudentId) {
        Project existing = projectRepository
                .findByIdAndStudentIdAndActiveTrue(id, authenticatedStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        existing.setTitle(request.title());
        existing.setDescription(request.description());

        Project saved = projectRepository.save(existing);
        return ProjectResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteProject(Integer id, Integer authenticatedStudentId) {
        Project existing = projectRepository
                .findByIdAndStudentIdAndActiveTrue(id, authenticatedStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        existing.setActive(false);
        existing.setStatus(ProjectStatus.DELETED);
        projectRepository.save(existing);
    }
}
