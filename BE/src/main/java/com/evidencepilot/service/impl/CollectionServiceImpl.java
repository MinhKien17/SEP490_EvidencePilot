package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.dto.response.CollectionResponse;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Collection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CollectionService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.dto.request.PagingRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private static final Set<String> COLLECTION_SORT_FIELDS = Set.of("title", "createdAt");

    private final CollectionRepository collectionRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public CollectionResponse createCollection(CollectionRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireRole(currentUser, UserRole.INSTRUCTOR);

        Project project = null;
        if (request.projectId() != null) {
            project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException(request.projectId(), "Project"));
            currentUserService.requireProjectWriteAccess(currentUser, project);
        }

        Collection collection = new Collection();
        collection.setTitle(request.name());
        collection.setDescription(request.description());
        collection.setProject(project);
        collection.setInstructor(currentUser);
        collection.setActive(true);
        collection.setCreatedAt(LocalDateTime.now());

        Collection saved = collectionRepository.save(collection);
        return toResponse(saved);
    }

    @Override
    public CollectionResponse getCollectionById(UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Collection"));
        currentUserService.requireCollectionAccess(currentUser, collection);
        return toResponse(collection);
    }

    @Override
    public List<CollectionResponse> getCollectionsByProjectId(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);
        return collectionRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PagedResponse<CollectionResponse> getCollectionsByProjectId(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            Boolean active) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);

        var pageable = PagingRequest.pageable(
                page, size, sort, COLLECTION_SORT_FIELDS, "createdAt,desc");
        var results = collectionRepository.findAll(
                collectionSpec(projectId, active, q),
                pageable);
        return PagedResponse.from(results.map(this::toResponse));
    }

    @Override
    @Transactional
    public void deleteCollection(UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Collection"));
        currentUserService.requireCollectionAccess(currentUser, collection);
        if (collection.getProject() != null) {
            currentUserService.requireProjectWriteAccess(currentUser, collection.getProject());
        }
        collection.setActive(false);
        collectionRepository.save(collection);
    }

    private CollectionResponse toResponse(Collection collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getTitle(),
                collection.getDescription(),
                collection.getProject() != null ? collection.getProject().getId() : null,
                collection.getCreatedAt());
    }

    private Specification<Collection> collectionSpec(UUID projectId, Boolean active, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));
            predicates.add(cb.equal(root.get("active"), active != null ? active : true));

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
