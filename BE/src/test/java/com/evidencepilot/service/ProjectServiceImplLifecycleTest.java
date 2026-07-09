package com.evidencepilot.service;

import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.ProjectMemberRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplLifecycleTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SystemNotificationService systemNotificationService;

    @Mock
    private ProjectMapper projectMapper;

    @Test
    void completeActiveProjectMarksCompleted() {
        User user = user();
        Project project = project(ProjectStatus.ACTIVE);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service().completeProject(project.getId());

        verify(currentUserService).requireProjectManageAccess(user, project);
        assertThat(response.status()).isEqualTo(ProjectStatus.COMPLETED);
    }

    @Test
    void completeRejectsDraftProject() {
        User user = user();
        Project project = project(ProjectStatus.DRAFT);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().completeProject(project.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only ACTIVE projects can be completed.");
    }

    @Test
    void archiveCompletedProjectMarksArchived() {
        User user = user();
        Project project = project(ProjectStatus.COMPLETED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service().archiveProject(project.getId());

        verify(currentUserService).requireProjectManageAccess(user, project);
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void archiveRejectsActiveProject() {
        User user = user();
        Project project = project(ProjectStatus.ACTIVE);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().archiveProject(project.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only COMPLETED projects can be archived.");
    }

    @Test
    void updateProjectRejectsCompletedProject() {
        User user = user();
        Project project = project(ProjectStatus.COMPLETED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().updateProject(
                project.getId(),
                new ProjectUpdateRequest("Updated", "Description", "IEEE")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
    }

    private ProjectServiceImpl service() {
        return new ProjectServiceImpl(
                projectRepository,
                projectMemberRepository,
                userRepository,
                currentUserService,
                systemNotificationService,
                projectMapper);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Project project(ProjectStatus status) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Capstone");
        project.setStatus(status);
        project.setActive(true);
        return project;
    }
}
