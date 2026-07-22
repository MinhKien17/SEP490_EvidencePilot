package com.evidencepilot.service;

import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
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

    @Mock
    private AuditService auditService;

    @Test
    void completeActiveProjectMarksCompleted() {
        User user = user();
        Project project = project(ProjectStatus.IN_PROGRESS);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service().completeProject(project.getId());

        verify(currentUserService).requireRole(user, UserRole.INSTRUCTOR);
        verify(currentUserService).requireProjectAccess(user, project);
        assertThat(response.status()).isEqualTo(ProjectStatus.APPROVED);
    }

    @Test
    void completeRejectsDraftProject() {
        User user = user();
        Project project = project(ProjectStatus.ASSIGNED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().completeProject(project.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project cannot be completed in its current state.");
    }

    @Test
    void archiveCompletedProjectMarksArchived() {
        User user = user();
        Project project = project(ProjectStatus.APPROVED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service().archiveProject(project.getId());

        verify(currentUserService).requireProjectManageAccess(user, project);
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(project.isActive()).isTrue();
        verify(auditService).record(
                "PROJECT_ARCHIVED", "PROJECT", project.getId(), user,
                ProjectStatus.APPROVED, ProjectStatus.ARCHIVED);
    }

    @Test
    void archiveRejectsActiveProject() {
        User user = user();
        Project project = project(ProjectStatus.IN_PROGRESS);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().archiveProject(project.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only APPROVED projects can be archived.")
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
        verify(projectRepository, never()).save(project);
        verify(auditService, never()).record(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unarchiveArchivedProjectMarksApprovedAndAudits() {
        User user = user();
        Project project = project(ProjectStatus.ARCHIVED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var response = service().unarchiveProject(project.getId());

        verify(currentUserService).requireProjectManageAccess(user, project);
        assertThat(response.status()).isEqualTo(ProjectStatus.APPROVED);
        assertThat(project.isActive()).isTrue();
        verify(auditService).record(
                "PROJECT_UNARCHIVED", "PROJECT", project.getId(), user,
                ProjectStatus.ARCHIVED, ProjectStatus.APPROVED);
    }

    @Test
    void unarchiveRejectsNonArchivedProjectWithoutSaveOrAudit() {
        User user = user();
        Project project = project(ProjectStatus.APPROVED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().unarchiveProject(project.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only ARCHIVED projects can be unarchived.")
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
        verify(projectRepository, never()).save(project);
        verify(auditService, never()).record(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateProjectRejectsCompletedProject() {
        User user = user();
        Project project = project(ProjectStatus.APPROVED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Project is read-only."))
                .when(currentUserService).requireProjectWriteAccess(user, project);

        assertThatThrownBy(() -> service().updateProject(
                project.getId(),
                new ProjectUpdateRequest("Updated", "Description", PaperStandard.IEEE)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
    }

    @Test
    void archivedProjectRejectsMetadataDeleteAndMemberMutations() {
        User user = user();
        Project project = project(ProjectStatus.ARCHIVED);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Project is read-only."))
                .when(currentUserService).requireProjectWriteAccess(user, project);

        assertReadOnly(() -> service().updateProject(
                project.getId(),
                new ProjectUpdateRequest("Updated", "Description", PaperStandard.IEEE)));
        assertReadOnly(() -> service().deleteProject(project.getId()));
        assertReadOnly(() -> service().addMember(project.getId(), UUID.randomUUID(), null));
        assertReadOnly(() -> service().removeMember(project.getId(), UUID.randomUUID()));

        verify(projectRepository, never()).save(project);
        verify(projectMemberRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(projectMemberRepository, never()).deleteAll(org.mockito.ArgumentMatchers.any());
        verify(currentUserService, times(4)).requireProjectWriteAccess(user, project);
    }

    @Test
    void getProjectByIdRequiresAccess() {
        User user = user();
        Project project = project(ProjectStatus.ARCHIVED);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        var response = service().getProjectById(project.getId());

        verify(currentUserService).requireProjectAccess(user, project);
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(project.isActive()).isTrue();
    }

    @Test
    void updateProjectChangesMutableMetadata() {
        User user = user();
        Project project = project(ProjectStatus.IN_PROGRESS);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        service().updateProject(project.getId(), new ProjectUpdateRequest("New", "Description", PaperStandard.CUSTOM));

        assertThat(project.getTitle()).isEqualTo("New");
        assertThat(project.getDescription()).isEqualTo("Description");
        verify(currentUserService).requireProjectManageAccess(user, project);
    }

    @Test
    void deleteProjectSoftDeletesAfterManageCheck() {
        User user = user();
        Project project = project(ProjectStatus.IN_PROGRESS);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        service().deleteProject(project.getId());

        assertThat(project.isActive()).isFalse();
        verify(projectRepository).save(project);
    }

    private ProjectServiceImpl service() {
        return new ProjectServiceImpl(
                projectRepository,
                projectMemberRepository,
                userRepository,
                currentUserService,
                systemNotificationService,
                projectMapper,
                auditService);
    }

    private void assertReadOnly(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
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
