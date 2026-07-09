package com.evidencepilot.service;

import com.evidencepilot.dto.response.FeedbackRequestResponseDto;
import com.evidencepilot.model.FeedbackRequest;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.FeedbackServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRequestRepository feedbackRequestRepository;

    @Mock
    private InstructorFeedbackRepository instructorFeedbackRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SystemNotificationService systemNotificationService;

    @Test
    void submitForReviewUsesProjectInstructorAndStudent() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(instructor, student);

        when(currentUserService.requireCurrentUser()).thenReturn(student);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(feedbackRequestRepository.save(any(FeedbackRequest.class))).thenAnswer(invocation -> {
            FeedbackRequest request = invocation.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });

        FeedbackRequestResponseDto response = service().submitForReview(project.getId(), null);

        verify(currentUserService).requireProjectWriteAccess(student, project);
        ArgumentCaptor<FeedbackRequest> requestCaptor = ArgumentCaptor.forClass(FeedbackRequest.class);
        verify(feedbackRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getInstructor()).isEqualTo(instructor);
        assertThat(requestCaptor.getValue().getStudent()).isEqualTo(student);
        assertThat(response.instructorId()).isEqualTo(instructor.getId());
        assertThat(response.studentId()).isEqualTo(student.getId());
    }

    private FeedbackServiceImpl service() {
        return new FeedbackServiceImpl(
                feedbackRequestRepository,
                instructorFeedbackRepository,
                projectRepository,
                userRepository,
                currentUserService,
                systemNotificationService);
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(user.getId() + "@example.com");
        user.setRole(role);
        return user;
    }

    private Project project(User instructor, User student) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Capstone");
        project.setActive(true);

        ProjectMember instructorMember = new ProjectMember();
        instructorMember.setProject(project);
        instructorMember.setUser(instructor);
        instructorMember.setRole(ProjectRole.INSTRUCTOR);

        ProjectMember studentMember = new ProjectMember();
        studentMember.setProject(project);
        studentMember.setUser(student);
        studentMember.setRole(ProjectRole.EDITOR);

        project.setProjectMembers(List.of(instructorMember, studentMember));
        return project;
    }
}
