package com.evidencepilot.service;

import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Collection;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectCollection;
import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectCollectionRepository;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.impl.ProjectCollectionService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectCollectionServiceTest {

    @Mock
    private ProjectCollectionRepository projectCollectionRepository;
    @Mock
    private ProjectDocumentRepository projectDocumentRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ClaimEvidenceMappingRepository claimEvidenceMappingRepository;
    @Mock
    private AiSuggestionRepository aiSuggestionRepository;
    @Mock
    private CurrentUserService currentUserService;

    @Test
    void linkMaterializesEveryCurrentSourceAndIsIdempotent() {
        User instructor = instructor();
        Project project = project(ProjectStatus.CREATED);
        Collection collection = collection(instructor);
        Document source = source(collection);
        ProjectCollection link = link(project, collection, instructor);

        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(projectCollectionRepository.findByProjectIdAndCollectionId(project.getId(), collection.getId()))
                .thenReturn(Optional.empty(), Optional.of(link));
        when(projectCollectionRepository.save(any(ProjectCollection.class))).thenReturn(link);
        when(documentRepository.findByCollectionIdAndDocTypeAndActiveTrue(
                collection.getId(), DocumentType.SOURCE)).thenReturn(List.of(source));
        when(projectDocumentRepository.findByProjectIdAndDocumentId(project.getId(), source.getId()))
                .thenReturn(Optional.empty(), Optional.of(projectDocument(project, source, link, false)));

        service().link(project.getId(), collection.getId());
        service().link(project.getId(), collection.getId());

        ArgumentCaptor<ProjectDocument> captor = ArgumentCaptor.forClass(ProjectDocument.class);
        verify(projectDocumentRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        ProjectDocument created = captor.getAllValues().getFirst();
        assertThat(created.getProjectCollection()).isEqualTo(link);
        assertThat(created.isPinned()).isFalse();
        verify(currentUserService, org.mockito.Mockito.times(2))
                .requireCollectionAccess(instructor, collection);
        verify(currentUserService, org.mockito.Mockito.times(2))
                .requireProjectWriteAccess(instructor, project);
    }

    @Test
    void syncSourceUpdatesAllWritableProjectsAndSkipsFrozenProjects() {
        User instructor = instructor();
        Collection collection = collection(instructor);
        Document source = source(collection);
        Project firstWritable = project(ProjectStatus.IN_PROGRESS);
        Project secondWritable = project(ProjectStatus.ASSIGNED);
        Project frozen = project(ProjectStatus.SUBMITTED_FOR_REVIEW);
        ProjectCollection firstWritableLink = link(firstWritable, collection, instructor);
        ProjectCollection secondWritableLink = link(secondWritable, collection, instructor);
        ProjectCollection frozenLink = link(frozen, collection, instructor);
        when(projectCollectionRepository.findByCollectionId(collection.getId()))
                .thenReturn(List.of(firstWritableLink, secondWritableLink, frozenLink));

        service().syncSource(source);

        ArgumentCaptor<ProjectDocument> captor = ArgumentCaptor.forClass(ProjectDocument.class);
        verify(projectDocumentRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ProjectDocument::getProject)
                .containsExactly(firstWritable, secondWritable);
        assertThat(captor.getAllValues()).allMatch(projectDocument -> !projectDocument.isPinned());
    }

    @Test
    void syncProjectBackfillsReturnedProjectWithoutChangingItsStatus() {
        User instructor = instructor();
        Project project = project(ProjectStatus.RETURNED);
        Collection collection = collection(instructor);
        Document source = source(collection);
        ProjectCollection link = link(project, collection, instructor);
        when(projectCollectionRepository.findByProjectId(project.getId())).thenReturn(List.of(link));
        when(documentRepository.findByCollectionIdAndDocTypeAndActiveTrue(
                collection.getId(), DocumentType.SOURCE)).thenReturn(List.of(source));

        service().syncProject(project);

        verify(projectDocumentRepository).save(any(ProjectDocument.class));
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.RETURNED);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void refreshProjectStatusCountsMaterializedCollectionSources() {
        Project project = project(ProjectStatus.ASSIGNED);
        Document paper = new Document();
        paper.setId(UUID.randomUUID());
        paper.setDocType(DocumentType.PAPER);
        paper.setActive(true);
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                project.getId(), DocumentType.PAPER)).thenReturn(List.of(paper));
        when(projectDocumentRepository.existsByProjectIdAndDocument_DocTypeAndDocument_ActiveTrue(
                project.getId(), DocumentType.SOURCE)).thenReturn(true);

        service().refreshProjectStatus(project);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        verify(projectRepository).save(project);
    }

    @Test
    void unlinkPinsMappedSourceInsteadOfRemovingIt() {
        User instructor = instructor();
        Project project = project(ProjectStatus.IN_PROGRESS);
        Collection collection = collection(instructor);
        Document source = source(collection);
        ProjectCollection link = link(project, collection, instructor);
        ProjectDocument projectDocument = projectDocument(project, source, link, false);
        stubAuthorizedLink(instructor, project, collection, link);
        when(projectDocumentRepository.findByProjectCollectionId(link.getId()))
                .thenReturn(List.of(projectDocument));
        when(claimEvidenceMappingRepository
                .existsByClaimProjectIdAndDocumentChunkDocumentIdAndStatus(
                        project.getId(), source.getId(), MappingStatus.ACTIVE))
                .thenReturn(true);

        service().unlink(project.getId(), collection.getId());

        assertThat(projectDocument.isPinned()).isTrue();
        assertThat(projectDocument.getProjectCollection()).isNull();
        verify(projectDocumentRepository).save(projectDocument);
        verify(projectDocumentRepository, never()).delete(projectDocument);
        verify(projectCollectionRepository).delete(link);
    }

    @Test
    void unlinkRemovesDerivedSourceAndInvalidatesPendingSuggestions() {
        User instructor = instructor();
        Project project = project(ProjectStatus.IN_PROGRESS);
        Collection collection = collection(instructor);
        Document source = source(collection);
        ProjectCollection link = link(project, collection, instructor);
        ProjectDocument projectDocument = projectDocument(project, source, link, false);
        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setStatus(SuggestionStatus.PENDING);
        stubAuthorizedLink(instructor, project, collection, link);
        when(projectDocumentRepository.findByProjectCollectionId(link.getId()))
                .thenReturn(List.of(projectDocument));
        when(aiSuggestionRepository.findByClaimProjectIdAndDocumentChunkDocumentIdAndStatus(
                project.getId(), source.getId(), SuggestionStatus.PENDING))
                .thenReturn(List.of(suggestion));

        service().unlink(project.getId(), collection.getId());

        verify(projectDocumentRepository).delete(projectDocument);
        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.INVALIDATED);
        verify(aiSuggestionRepository).saveAll(List.of(suggestion));
    }

    @Test
    void deletingCollectionPreservesSharedSourcesAsPinnedStandaloneDocuments() {
        User instructor = instructor();
        Project project = project(ProjectStatus.APPROVED);
        Collection collection = collection(instructor);
        Document source = source(collection);
        ProjectCollection link = link(project, collection, instructor);
        ProjectDocument projectDocument = projectDocument(project, source, link, false);
        when(projectCollectionRepository.findByCollectionId(collection.getId())).thenReturn(List.of(link));
        when(projectDocumentRepository.findByProjectCollectionId(link.getId()))
                .thenReturn(List.of(projectDocument));
        when(documentRepository.findByCollectionIdAndDocTypeAndActiveTrue(
                collection.getId(), DocumentType.SOURCE)).thenReturn(List.of(source));
        when(projectDocumentRepository.existsByDocumentId(source.getId())).thenReturn(true);

        service().prepareCollectionDeletion(collection);

        assertThat(projectDocument.isPinned()).isTrue();
        assertThat(projectDocument.getProjectCollection()).isNull();
        assertThat(source.getCollection()).isNull();
        verify(projectCollectionRepository).deleteAll(List.of(link));
        verify(documentRepository).saveAll(List.of(source));
    }

    @Test
    void manualSharePinsInheritedSourceAndManualUnshareOnlyClearsPin() {
        User instructor = instructor();
        Project project = project(ProjectStatus.IN_PROGRESS);
        Collection collection = collection(instructor);
        Document source = source(collection);
        ProjectCollection link = link(project, collection, instructor);
        ProjectDocument projectDocument = projectDocument(project, source, link, false);
        when(projectCollectionRepository.findByProjectIdAndCollectionId(project.getId(), collection.getId()))
                .thenReturn(Optional.of(link));
        when(projectDocumentRepository.findByProjectIdAndDocumentId(project.getId(), source.getId()))
                .thenReturn(Optional.of(projectDocument));

        service().pinSource(project, source, instructor);
        assertThat(projectDocument.isPinned()).isTrue();

        service().unshare(projectDocument);
        assertThat(projectDocument.isPinned()).isFalse();
        assertThat(projectDocument.getProjectCollection()).isEqualTo(link);
        verify(projectDocumentRepository, never()).delete(projectDocument);
    }

    @Test
    void movingSourceDetachesOldDerivedShareBeforeSyncingNewCollection() {
        User instructor = instructor();
        Project project = project(ProjectStatus.IN_PROGRESS);
        Collection oldCollection = collection(instructor);
        Collection newCollection = collection(instructor);
        Document source = source(oldCollection);
        ProjectCollection oldLink = link(project, oldCollection, instructor);
        ProjectDocument projectDocument = projectDocument(project, source, oldLink, false);
        when(projectDocumentRepository.findByDocumentId(source.getId()))
                .thenReturn(List.of(projectDocument));
        when(documentRepository.save(source)).thenReturn(source);

        service().moveSource(source, newCollection);

        assertThat(source.getCollection()).isEqualTo(newCollection);
        verify(projectDocumentRepository).delete(projectDocument);
        verify(projectCollectionRepository).findByCollectionId(newCollection.getId());
    }

    @Test
    void frozenProjectRejectsLinkEvenForAuthorizedCaller() {
        User instructor = instructor();
        Project project = project(ProjectStatus.SUBMITTED_FOR_REVIEW);
        Collection collection = collection(instructor);
        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        assertThatThrownBy(() -> service().link(project.getId(), collection.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("corpus is locked");
        verify(projectCollectionRepository, never()).save(any(ProjectCollection.class));
    }

    private void stubAuthorizedLink(User user, Project project, Collection collection, ProjectCollection link) {
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(projectCollectionRepository.findByProjectIdAndCollectionId(project.getId(), collection.getId()))
                .thenReturn(Optional.of(link));
    }

    private ProjectCollectionService service() {
        return new ProjectCollectionService(
                projectCollectionRepository,
                projectDocumentRepository,
                projectRepository,
                collectionRepository,
                documentRepository,
                claimEvidenceMappingRepository,
                aiSuggestionRepository,
                currentUserService);
    }

    private User instructor() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.INSTRUCTOR);
        user.setEmail(user.getId() + "@example.com");
        return user;
    }

    private Project project(ProjectStatus status) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Project");
        project.setStatus(status);
        project.setActive(true);
        return project;
    }

    private Collection collection(User instructor) {
        Collection collection = new Collection();
        collection.setId(UUID.randomUUID());
        collection.setInstructor(instructor);
        collection.setTitle("Collection");
        collection.setActive(true);
        return collection;
    }

    private Document source(Collection collection) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setCollection(collection);
        document.setDocType(DocumentType.SOURCE);
        document.setActive(true);
        return document;
    }

    private ProjectCollection link(Project project, Collection collection, User user) {
        ProjectCollection link = new ProjectCollection();
        link.setId(UUID.randomUUID());
        link.setProject(project);
        link.setCollection(collection);
        link.setLinkedBy(user);
        return link;
    }

    private ProjectDocument projectDocument(
            Project project, Document source, ProjectCollection link, boolean pinned) {
        ProjectDocument projectDocument = new ProjectDocument();
        projectDocument.setId(UUID.randomUUID());
        projectDocument.setProject(project);
        projectDocument.setDocument(source);
        projectDocument.setProjectCollection(link);
        projectDocument.setPinned(pinned);
        return projectDocument;
    }
}
