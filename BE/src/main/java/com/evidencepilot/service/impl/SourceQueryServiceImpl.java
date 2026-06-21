package com.evidencepilot.service.impl;

import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.response.SourceResponseDto;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.SourceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceQueryServiceImpl implements SourceQueryService {

    private final SourceRepository sourceRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    @Transactional(readOnly = true)
    public List<SourceResponseDto> getSourcesByProject(Integer projectId) {
        User currentUser = currentUserService.requireCurrentUser();

        if (currentUserService.isAdmin(currentUser)) {
            if (!projectRepository.existsById(projectId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
            }
        } else if (currentUserService.isInstructor(currentUser)) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
            currentUserService.requireProjectAccess(currentUser, project);
        } else {
            if (!projectRepository.existsByIdAndStudentIdAndActiveTrue(projectId, currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
            }
        }

        return sourceRepository.findByProjectIdAndActiveTrue(projectId).stream()
                .map(SourceResponseDto::fromEntity)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    @Transactional(readOnly = true)
    public SourceResponseDto getProjectSource(Integer projectId, Integer sourceId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
        currentUserService.requireProjectAccess(currentUser, project);

        Source source = sourceRepository.findByIdAndProjectIdAndActiveTrue(sourceId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found in this project"));

        return SourceResponseDto.fromEntity(source);
    }
}
