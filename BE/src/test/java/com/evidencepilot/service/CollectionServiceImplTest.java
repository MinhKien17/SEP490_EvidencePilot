package com.evidencepilot.service;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.model.Collection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.impl.CollectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceImplTest {

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Test
    void createCollectionRequiresInstructorRoleAndProjectAccess() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = new Project();
        project.setId(UUID.randomUUID());
        CollectionRequest request = new CollectionRequest("Evidence", "Notes", project.getId());

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection collection = invocation.getArgument(0);
            collection.setId(UUID.randomUUID());
            return collection;
        });

        var response = service().createCollection(request);

        assertThat(response.name()).isEqualTo("Evidence");
        verify(currentUserService).requireRole(instructor, UserRole.INSTRUCTOR);
        verify(currentUserService).requireProjectWriteAccess(instructor, project);
    }

    @Test
    void getCollectionByIdChecksCollectionAccess() {
        User instructor = user(UserRole.INSTRUCTOR);
        Collection collection = collection(instructor);

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        service().getCollectionById(collection.getId());

        verify(currentUserService).requireCollectionAccess(instructor, collection);
    }

    @Test
    void getCollectionsByProjectIdChecksProjectAccess() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setStatus(ProjectStatus.ARCHIVED);

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(collectionRepository.findByProjectId(project.getId()))
                .thenReturn(List.of(collection(instructor)));

        service().getCollectionsByProjectId(project.getId());

        verify(currentUserService).requireProjectAccess(instructor, project);
    }

    @Test
    void deleteCollectionChecksCollectionAccess() {
        User instructor = user(UserRole.INSTRUCTOR);
        Collection collection = collection(instructor);

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        service().deleteCollection(collection.getId());

        verify(currentUserService).requireCollectionAccess(instructor, collection);
    }

    @Test
    void archivedProjectRejectsCollectionCreateAndDelete() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setStatus(ProjectStatus.ARCHIVED);
        Collection collection = collection(instructor);
        collection.setProject(project);
        CollectionRequest request = new CollectionRequest("Evidence", "Notes", project.getId());

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Project is read-only."))
                .when(currentUserService).requireProjectWriteAccess(instructor, project);

        assertThatThrownBy(() -> service().createCollection(request))
                .hasMessageContaining("Project is read-only.");
        assertThatThrownBy(() -> service().deleteCollection(collection.getId()))
                .hasMessageContaining("Project is read-only.");
        verify(collectionRepository, never()).save(any());
    }

    private CollectionServiceImpl service() {
        return new CollectionServiceImpl(collectionRepository, projectRepository, currentUserService);
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setEmail(user.getId() + "@example.com");
        return user;
    }

    private Collection collection(User instructor) {
        Collection collection = new Collection();
        collection.setId(UUID.randomUUID());
        collection.setTitle("Evidence");
        collection.setInstructor(instructor);
        collection.setActive(true);
        return collection;
    }
}
