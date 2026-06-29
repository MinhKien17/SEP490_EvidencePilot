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
    void requireProjectWriteAccessStillRejectsInstructor() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = projectOwnedBy(user(UserRole.STUDENT));

        CurrentUserServiceImpl service =
                new CurrentUserServiceImpl(userRepository, feedbackRequestRepository);

        assertThatThrownBy(() -> service.requireProjectWriteAccess(instructor, project))
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
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setActive(true);

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(owner);
        member.setRole(ProjectRole.OWNER);
        project.setProjectMembers(List.of(member));
        return project;
    }
}
