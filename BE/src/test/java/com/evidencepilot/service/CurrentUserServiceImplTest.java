package com.evidencepilot.service;

import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.CurrentUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackRequestRepository feedbackRequestRepository;

    @Test
    void requireProjectAccessAllowsAssignedInstructorDuringReview() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = projectOwnedBy(user(UserRole.STUDENT));
        project.setStatus(ProjectStatus.IN_REVIEW);

        when(feedbackRequestRepository.existsByProjectIdAndInstructorId(
                project.getId(), instructor.getId())).thenReturn(true);

        CurrentUserServiceImpl service =
                new CurrentUserServiceImpl(userRepository, feedbackRequestRepository);

        assertThatCode(() -> service.requireProjectAccess(instructor, project))
                .doesNotThrowAnyException();
    }

    @Test
    void requireProjectWriteAccessAllowsInstructorMember() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = projectWithMembers(member(instructor, ProjectRole.INSTRUCTOR));

        CurrentUserServiceImpl service =
                new CurrentUserServiceImpl(userRepository, feedbackRequestRepository);

        assertThatCode(() -> service.requireProjectWriteAccess(instructor, project))
                .doesNotThrowAnyException();
    }

    @Test
    void requireProjectWriteAccessAllowsStudentEditor() {
        User student = user(UserRole.STUDENT);
        Project project = projectWithMembers(member(student, ProjectRole.EDITOR));

        CurrentUserServiceImpl service =
                new CurrentUserServiceImpl(userRepository, feedbackRequestRepository);

        assertThatCode(() -> service.requireProjectWriteAccess(student, project))
                .doesNotThrowAnyException();
    }

    @Test
    void requireProjectManageAccessRejectsStudentEditor() {
        User student = user(UserRole.STUDENT);
        Project project = projectWithMembers(member(student, ProjectRole.EDITOR));

        CurrentUserServiceImpl service =
                new CurrentUserServiceImpl(userRepository, feedbackRequestRepository);

        assertThatThrownBy(() -> service.requireProjectManageAccess(student, project))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setEmail(user.getId() + "@example.com");
        return user;
    }

    private Project projectOwnedBy(User owner) {
        return projectWithMembers(member(owner, ProjectRole.OWNER));
    }

    private Project projectWithMembers(ProjectMember... members) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setActive(true);
        project.setProjectMembers(List.of(members));
        project.getProjectMembers().forEach(member -> member.setProject(project));
        return project;
    }

    private ProjectMember member(User user, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setUser(user);
        member.setRole(role);
        return member;
    }
}
