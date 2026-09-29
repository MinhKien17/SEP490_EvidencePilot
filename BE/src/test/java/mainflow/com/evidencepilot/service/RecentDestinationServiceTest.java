package com.evidencepilot.service;

import com.evidencepilot.dto.response.RecentDestinationResponse;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.RecentDestinationRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.CurrentUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentDestinationServiceTest {

    @Mock private RecentDestinationRepository recentDestinations;
    @Mock private UserRepository users;
    @Mock private ProjectRepository projects;
    @Mock private CollectionRepository collections;
    @Mock private DocumentRepository documents;
    @Mock private CurrentUserServiceImpl currentUsers;

    @InjectMocks private RecentDestinationService service;

    @Test
    void record_rejectsUnknownKind() {
        User user = user(UserRole.STUDENT);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.record(user.getId(), "NOPE", UUID.randomUUID(), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void record_returnsNullForInaccessibleProject() {
        User student = user(UserRole.STUDENT);
        Project project = project();
        when(users.findById(student.getId())).thenReturn(Optional.of(student));
        when(projects.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "denied"))
                .when(currentUsers).requireProjectAccess(student, project);

        assertThat(service.record(student.getId(), "project", project.getId(), null)).isNull();
    }

    @Test
    void record_linksByRoleAndPrunesBeyondCap() {
        User student = user(UserRole.STUDENT);
        Project project = project();
        when(users.findById(student.getId())).thenReturn(Optional.of(student));
        when(projects.findById(project.getId())).thenReturn(Optional.of(project));
        when(recentDestinations.findByUserIdAndKindAndRefIdAndTab(
                student.getId(), "PROJECT", project.getId(), "")).thenReturn(List.of());
        when(recentDestinations.save(any())).thenAnswer(i -> i.getArgument(0));
        when(recentDestinations.findByUserIdOrderByLastOpenedAtDesc(student.getId()))
                .thenReturn(List.of());

        RecentDestinationResponse response = service.record(student.getId(), "PROJECT", project.getId(), null);

        assertThat(response.link()).isEqualTo("/student/projects/" + project.getId());
        assertThat(response.label()).isEqualTo("Seed");
        verify(recentDestinations).save(any());
    }

    @Test
    void record_sourceLibraryRejectedForStudents() {
        User student = user(UserRole.STUDENT);
        when(users.findById(student.getId())).thenReturn(Optional.of(student));

        assertThat(service.record(student.getId(), "SOURCE_LIBRARY", null, null)).isNull();
    }

    @Test
    void list_dropsInaccessibleSilently() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = project();
        com.evidencepilot.model.RecentDestination entry = new com.evidencepilot.model.RecentDestination();
        entry.setUser(instructor);
        entry.setKind("PROJECT");
        entry.setRefId(project.getId());
        entry.setLink("/instructor/projects/" + project.getId());
        when(users.findById(instructor.getId())).thenReturn(Optional.of(instructor));
        when(recentDestinations.findByUserIdOrderByLastOpenedAtDesc(instructor.getId()))
                .thenReturn(new java.util.ArrayList<>(List.of(entry)));
        when(projects.findById(project.getId())).thenReturn(Optional.empty());

        assertThat(service.list(instructor.getId())).isEmpty();
        verify(recentDestinations).delete(entry);
    }

    private static User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    private static Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Seed");
        project.setActive(true);
        return project;
    }
}
