package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.CollectionResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Collection;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectCollection;
import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectCollectionRepository;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectCollectionService {

    private static final Set<ProjectStatus> SYNC_PAUSED_STATUSES = Set.of(
            ProjectStatus.SUBMITTED_FOR_REVIEW,
            ProjectStatus.APPROVED,
            ProjectStatus.ARCHIVED);

    private final ProjectCollectionRepository projectCollectionRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository;
    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;

    public List<CollectionResponse> getLinkedCollections(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = requireActiveProject(projectId);
        currentUserService.requireProjectAccess(currentUser, project);
        return projectCollectionRepository.findByProjectId(projectId).stream()
                .map(ProjectCollection::getCollection)
                .filter(Collection::isActive)
                .map(CollectionResponse::from)
                .toList();
    }

    @Transactional
    public CollectionResponse link(UUID projectId, UUID collectionId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = requireActiveProject(projectId);
        Collection collection = requireActiveCollection(collectionId);
        currentUserService.requireCollectionAccess(currentUser, collection);
        requireSyncWriteAccess(currentUser, project);

        ProjectCollection link = projectCollectionRepository
                .findByProjectIdAndCollectionId(projectId, collectionId)
                .orElseGet(() -> {
                    ProjectCollection created = new ProjectCollection();
                    created.setProject(project);
                    created.setCollection(collection);
                    created.setLinkedBy(currentUser);
                    created.setLinkedAt(LocalDateTime.now());
                    return projectCollectionRepository.save(created);
                });

        syncCollection(link);
        return CollectionResponse.from(collection);
    }

    @Transactional
    public void unlink(UUID projectId, UUID collectionId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = requireActiveProject(projectId);
        Collection collection = requireActiveCollection(collectionId);
        currentUserService.requireCollectionAccess(currentUser, collection);
        requireSyncWriteAccess(currentUser, project);

        projectCollectionRepository.findByProjectIdAndCollectionId(projectId, collectionId)
                .ifPresent(link -> {
                    projectDocumentRepository.findByProjectCollectionId(link.getId())
                            .forEach(this::detachDerivedShare);
                    projectCollectionRepository.delete(link);
                });
    }

    @Transactional
    public void syncSource(Document document) {
        if (document == null || document.getCollection() == null
                || !document.getCollection().isActive()
                || document.getDocType() != DocumentType.SOURCE || !document.isActive()) {
            return;
        }
        for (ProjectCollection link : projectCollectionRepository
                .findByCollectionId(document.getCollection().getId())) {
            Project project = link.getProject();
            if (project.isActive() && !isSyncPaused(project)) {
                materialize(link, document);
            }
        }
    }

    @Transactional
    public void syncProject(Project project) {
        if (project == null || !project.isActive() || isSyncPaused(project)) {
            return;
        }
        for (ProjectCollection link : projectCollectionRepository.findByProjectId(project.getId())) {
            syncCollection(link);
        }
    }

    @Transactional
    public void prepareCollectionDeletion(Collection collection) {
        List<ProjectCollection> links = projectCollectionRepository.findByCollectionId(collection.getId());
        for (ProjectCollection link : links) {
            for (ProjectDocument projectDocument : projectDocumentRepository
                    .findByProjectCollectionId(link.getId())) {
                projectDocument.setPinned(true);
                projectDocument.setProjectCollection(null);
                projectDocumentRepository.save(projectDocument);
            }
        }
        projectCollectionRepository.deleteAll(links);

        List<Document> retained = documentRepository
                .findByCollectionIdAndDocTypeAndActiveTrue(collection.getId(), DocumentType.SOURCE).stream()
                .filter(document -> projectDocumentRepository.existsByDocumentId(document.getId()))
                .peek(document -> document.setCollection(null))
                .toList();
        documentRepository.saveAll(retained);
    }

    @Transactional
    public Document moveSource(Document document, Collection targetCollection) {
        if (document.getCollection() != null
                && document.getCollection().getId().equals(targetCollection.getId())) {
            syncSource(document);
            return document;
        }

        for (ProjectDocument projectDocument : projectDocumentRepository.findByDocumentId(document.getId())) {
            ProjectCollection link = projectDocument.getProjectCollection();
            if (link != null && !link.getCollection().getId().equals(targetCollection.getId())) {
                requireCorpusMutable(link.getProject());
                detachDerivedShare(projectDocument);
            }
        }

        document.setCollection(targetCollection);
        Document saved = documentRepository.save(document);
        syncSource(saved);
        return saved;
    }

    @Transactional
    public void removeSource(Document document) {
        for (ProjectDocument projectDocument : projectDocumentRepository.findByDocumentId(document.getId())) {
            requireCorpusMutable(projectDocument.getProject());
            projectDocumentRepository.delete(projectDocument);
        }
    }

    @Transactional
    public void pinSource(Project project, Document document, User sharedBy) {
        requireCorpusMutable(project);
        ProjectCollection link = document.getCollection() == null ? null
                : projectCollectionRepository
                        .findByProjectIdAndCollectionId(project.getId(), document.getCollection().getId())
                        .orElse(null);
        ProjectDocument projectDocument = projectDocumentRepository
                .findByProjectIdAndDocumentId(project.getId(), document.getId())
                .orElseGet(() -> {
                    ProjectDocument created = new ProjectDocument();
                    created.setProject(project);
                    created.setDocument(document);
                    created.setSharedBy(sharedBy);
                    created.setSharedAt(LocalDateTime.now());
                    return created;
                });
        projectDocument.setPinned(true);
        if (projectDocument.getProjectCollection() == null) {
            projectDocument.setProjectCollection(link);
        }
        projectDocumentRepository.save(projectDocument);
    }

    @Transactional
    public void unshare(ProjectDocument projectDocument) {
        Project project = projectDocument.getProject();
        requireCorpusMutable(project);
        if (projectDocument.getProjectCollection() != null) {
            projectDocument.setPinned(false);
            projectDocumentRepository.save(projectDocument);
            return;
        }
        projectDocumentRepository.delete(projectDocument);
    }

    private void syncCollection(ProjectCollection link) {
        documentRepository.findByCollectionIdAndDocTypeAndActiveTrue(
                        link.getCollection().getId(), DocumentType.SOURCE)
                .forEach(document -> materialize(link, document));
    }

    private void materialize(ProjectCollection link, Document document) {
        ProjectDocument projectDocument = projectDocumentRepository
                .findByProjectIdAndDocumentId(link.getProject().getId(), document.getId())
                .orElse(null);
        if (projectDocument == null) {
            projectDocument = new ProjectDocument();
            projectDocument.setProject(link.getProject());
            projectDocument.setDocument(document);
            projectDocument.setSharedBy(link.getLinkedBy());
            projectDocument.setSharedAt(LocalDateTime.now());
            projectDocument.setPinned(false);
        } else if (projectDocument.getProjectCollection() == null) {
            projectDocument.setPinned(true);
        }
        projectDocument.setProjectCollection(link);
        projectDocumentRepository.save(projectDocument);
    }

    private void detachDerivedShare(ProjectDocument projectDocument) {
        if (projectDocument.isPinned()) {
            projectDocument.setPinned(true);
            projectDocument.setProjectCollection(null);
            projectDocumentRepository.save(projectDocument);
            return;
        }
        projectDocumentRepository.delete(projectDocument);
    }

    private Project requireActiveProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        if (!project.isActive()) {
            throw new ResourceNotFoundException(projectId, "Project");
        }
        return project;
    }

    private Collection requireActiveCollection(UUID collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException(collectionId, "Collection"));
        if (!collection.isActive()) {
            throw new ResourceNotFoundException(collectionId, "Collection");
        }
        return collection;
    }

    private void requireSyncWriteAccess(User currentUser, Project project) {
        currentUserService.requireProjectWriteAccess(currentUser, project);
        requireCorpusMutable(project);
    }

    private void requireCorpusMutable(Project project) {
        if (isSyncPaused(project)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Project corpus is locked and cannot be modified.");
        }
    }

    private boolean isSyncPaused(Project project) {
        return SYNC_PAUSED_STATUSES.contains(project.getStatus());
    }
}
