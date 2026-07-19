package com.evidencepilot.service;

import com.evidencepilot.dto.response.FeedbackRequestResponseDto;
import com.evidencepilot.model.FeedbackRequest;
import com.evidencepilot.dto.request.InstructorFeedbackRequest;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.User;
import com.evidencepilot.model.FeedbackStatus;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.FeedbackServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRequestRepository feedbackRequestRepository;

    @Mock
    private InstructorFeedbackRepository instructorFeedbackRepository;

    @Mock
    private PaperSectionRepository paperSectionRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SystemNotificationService systemNotificationService;

    @Mock
    private PaperProcessingService paperProcessingService;

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

    @Test
    void commentCreatesSeparateFeedbackForEachPaperSectionInSameRequest() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(instructor, student);
        FeedbackRequest request = feedbackRequest(project, instructor, student);
        PaperSection intro = section(project, "Introduction");
        PaperSection method = section(project, "Method");

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(feedbackRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(paperSectionRepository.findById(intro.getId())).thenReturn(Optional.of(intro));
        when(paperSectionRepository.findById(method.getId())).thenReturn(Optional.of(method));
        when(instructorFeedbackRepository.save(any(InstructorFeedback.class))).thenAnswer(invocation -> {
            InstructorFeedback feedback = invocation.getArgument(0);
            feedback.setId(UUID.randomUUID());
            return feedback;
        });

        service().comment(request.getId(), new InstructorFeedbackRequest(intro.getId(), "L1", "Tighten intro."));
        service().comment(request.getId(), new InstructorFeedbackRequest(method.getId(), "L2", "Clarify method."));

        ArgumentCaptor<InstructorFeedback> captor = ArgumentCaptor.forClass(InstructorFeedback.class);
        verify(instructorFeedbackRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(feedback -> feedback.getSection().getId())
                .containsExactly(intro.getId(), method.getId());
        assertThat(captor.getAllValues())
                .extracting(InstructorFeedback::getLineReference)
                .containsExactly("L1", "L2");
        assertThat(captor.getAllValues())
                .allSatisfy(feedback -> assertThat(feedback.getRequest()).isEqualTo(request));
    }

    @Test
    void commentRejectsSectionOutsideFeedbackProject() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(instructor, student);
        Project otherProject = project(instructor, student);
        FeedbackRequest request = feedbackRequest(project, instructor, student);
        PaperSection otherSection = section(otherProject, "Other");

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(feedbackRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(paperSectionRepository.findById(otherSection.getId())).thenReturn(Optional.of(otherSection));

        assertThatThrownBy(() -> service().comment(
                request.getId(),
                new InstructorFeedbackRequest(otherSection.getId(), "L1", "Wrong project.")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Section does not belong to feedback project.");
    }

    @Test
    void submitForReviewRejectsProjectAlreadyInReview() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(instructor, student);
        project.setStatus(ProjectStatus.SUBMITTED_FOR_REVIEW);

        when(currentUserService.requireCurrentUser()).thenReturn(student);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().submitForReview(project.getId(), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project is already in review.");
    }

    @Test
    void submitForReviewRejectsCompletedProject() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(instructor, student);
        project.setStatus(ProjectStatus.APPROVED);

        when(currentUserService.requireCurrentUser()).thenReturn(student);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service().submitForReview(project.getId(), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only ACTIVE projects can be submitted for review.");
    }

    @Test
    void commentRejectsClosedFeedbackRequest() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(instructor, student);
        FeedbackRequest request = feedbackRequest(project, instructor, student);
        request.setStatus(FeedbackStatus.REVIEWED);

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(feedbackRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service().comment(
                request.getId(),
                new InstructorFeedbackRequest(UUID.randomUUID(), "L1", "Too late.")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Feedback request closed.");
    }

    @Test
    void findAllForCurrentUserUsesRoleScopedRepository() {
        User student = user(UserRole.STUDENT);
        when(currentUserService.requireCurrentUser()).thenReturn(student);
        when(feedbackRequestRepository.findByStudentId(student.getId())).thenReturn(List.of());

        assertThat(service().findAllForCurrentUser()).isEmpty();

        verify(feedbackRequestRepository).findByStudentId(student.getId());
    }

    @Test
    void updateStatusTransitionsRequestAndNotifiesStudent() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(instructor, student);
        FeedbackRequest request = feedbackRequest(project, instructor, student);
        request.setStatus(FeedbackStatus.PENDING);
        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(feedbackRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(feedbackRequestRepository.save(request)).thenReturn(request);

        service().updateStatus(request.getId(), "REVIEWED");

        assertThat(request.getStatus()).isEqualTo(FeedbackStatus.REVIEWED);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.RETURNED);
        verify(systemNotificationService).createNotification(
                student, instructor, "REVIEW_STATUS_CHANGED", request.getId(),
                "Review status for project \"Capstone\" changed to REVIEWED.");
    }

    @Test
    void updateStatusRejectsUnknownAndPendingValues() {
        assertThatThrownBy(() -> service().updateStatus(UUID.randomUUID(), "unknown"))
                .hasMessageContaining("Invalid status");
        assertThatThrownBy(() -> service().updateStatus(UUID.randomUUID(), "PENDING"))
                .hasMessageContaining("Invalid status");
    }

    private FeedbackServiceImpl service() {
        return new FeedbackServiceImpl(
                feedbackRequestRepository,
                instructorFeedbackRepository,
                paperSectionRepository,
                documentRepository,
                projectRepository,
                userRepository,
                currentUserService,
                systemNotificationService,
                paperProcessingService);
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
        project.setStatus(ProjectStatus.IN_PROGRESS);

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

    private FeedbackRequest feedbackRequest(Project project, User instructor, User student) {
        FeedbackRequest request = new FeedbackRequest();
        request.setId(UUID.randomUUID());
        request.setProject(project);
        request.setInstructor(instructor);
        request.setStudent(student);
        return request;
    }

    private PaperSection section(Project project, String title) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);

        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        section.setSectionTitle(title);
        section.setSectionOrder(1);
        section.setContentTex(title);
        return section;
    }
}
