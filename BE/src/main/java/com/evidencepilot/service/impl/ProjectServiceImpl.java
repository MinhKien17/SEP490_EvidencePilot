package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.dto.response.ProjectMemberResponse;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.ProjectMemberRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.ProjectService;
import com.evidencepilot.service.SystemNotificationService;
import com.evidencepilot.dto.request.PagingRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final Set<String> PROJECT_SORT_FIELDS = Set.of(
            "title", "status", "createdAt", "updatedAt");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final SystemNotificationService systemNotificationService;
    private final ProjectMapper projectMapper;

    @Override
    public List<ProjectResponse> getAllProjects() {
        User currentUser = currentUserService.requireCurrentUser();
        return projectMemberRepository.findByUserId(currentUser.getId()).stream()
                .map(ProjectMember::getProject)
                .filter(Project::isActive)
                .map(ProjectResponse::from)
                .toList();
    }

    @Override
    public PagedResponse<ProjectResponse> getAllProjects(
            int page,
            int size,
            String sort,
            String q,
            ProjectStatus status,
            Boolean active) {
        User currentUser = currentUserService.requireCurrentUser();
        var pageable = PagingRequest.pageable(
                page, size, sort, PROJECT_SORT_FIELDS, "createdAt,desc");
        var results = projectRepository.findAll(
                projectSpec(currentUser, q, status, active),
                pageable);
        return PagedResponse.from(results.map(ProjectResponse::from));
    }

    @Override
    public ProjectResponse getProjectById(UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(id);
        currentUserService.requireProjectAccess(currentUser, project);
        return ProjectResponse.from(project);
    }

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireRole(currentUser, UserRole.INSTRUCTOR);

        Project project = new Project();
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setTargetStandard(request.targetStandard());
        project.setStatus(ProjectStatus.ASSIGNED);
        project.setCreatedAt(LocalDateTime.now());

        Project saved = projectRepository.save(project);

        ProjectMember owner = new ProjectMember();
        owner.setProject(saved);
        owner.setUser(currentUser);
        owner.setRole(currentUser.getRole() == UserRole.INSTRUCTOR
                ? ProjectRole.INSTRUCTOR
                : ProjectRole.OWNER);
        owner.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(owner);

        return ProjectResponse.from(saved);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(UUID id, ProjectUpdateRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(id);
        currentUserService.requireProjectManageAccess(currentUser, project);
        requireMutable(project);

        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setTargetStandard(request.targetStandard());
        project.setUpdatedAt(LocalDateTime.now());

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Override
    @Transactional
    public ProjectResponse completeProject(UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(id);
        currentUserService.requireProjectManageAccess(currentUser, project);
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only ACTIVE projects can be completed.");
        }
        project.setStatus(ProjectStatus.APPROVED);
        project.setUpdatedAt(LocalDateTime.now());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Override
    @Transactional
    public ProjectResponse archiveProject(UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(id);
        currentUserService.requireProjectManageAccess(currentUser, project);
        if (project.getStatus() != ProjectStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only COMPLETED projects can be archived.");
        }
        project.setStatus(ProjectStatus.APPROVED);
        project.setUpdatedAt(LocalDateTime.now());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProject(UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(id);
        currentUserService.requireProjectManageAccess(currentUser, project);
        project.setActive(false);
        projectRepository.save(project);
    }

    @Override
    public List<ProjectMember> getProjectMembers(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(projectId);
        currentUserService.requireProjectAccess(currentUser, project);
        return projectMemberRepository.findByProjectId(projectId);
    }

    @Override
    public List<ProjectMemberResponse> getProjectMemberResponses(UUID projectId) {
        return getProjectMembers(projectId).stream()
                .map(projectMapper::toProjectMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public void addMember(UUID projectId, UUID userId, ProjectRole role) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(projectId);
        currentUserService.requireProjectManageAccess(currentUser, project);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(userId, "User"));
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only students can be added to projects.");
        }
        if (!projectMemberRepository.findByProjectIdAndUserId(projectId, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a project member.");
        }
        ProjectRole memberRole = role != null ? role : ProjectRole.EDITOR;
        if (memberRole != ProjectRole.EDITOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Students must be EDITOR.");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(memberRole);
        member.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(member);
        systemNotificationService.createNotification(
                user,
                currentUser,
                "PROJECT_MEMBER_ADDED",
                project.getId(),
                currentUser.getEmail() + " added you to project \"" + project.getTitle() + "\".");
    }

    @Override
    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = findActiveProject(projectId);
        currentUserService.requireProjectManageAccess(currentUser, project);
        List<ProjectMember> members = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
        members.forEach(member -> systemNotificationService.createNotification(
                member.getUser(),
                currentUser,
                "PROJECT_MEMBER_REMOVED",
                project.getId(),
                currentUser.getEmail() + " removed you from project \"" + project.getTitle() + "\"."));
        projectMemberRepository.deleteAll(members);
    }

    private Project findActiveProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Project"));
        if (!project.isActive()) {
            throw new ResourceNotFoundException(id, "Project");
        }
        return project;
    }

    private void requireMutable(Project project) {
        if (project.getStatus() == ProjectStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project is read-only.");
        }
    }

    private Specification<Project> projectSpec(
            User currentUser,
            String q,
            ProjectStatus status,
            Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("active"), active != null ? active : true));

            if (!currentUserService.isAdmin(currentUser)) {
                if (query != null) {
                    query.distinct(true);
                }
                var members = root.join("projectMembers");
                predicates.add(cb.equal(members.get("user").get("id"), currentUser.getId()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("targetStandard")), like)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
