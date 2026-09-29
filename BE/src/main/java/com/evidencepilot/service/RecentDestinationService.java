package com.evidencepilot.service;

import com.evidencepilot.dto.response.RecentDestinationResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Collection;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.RecentDestination;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.RecentDestinationRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.CurrentUserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Phase B: private recent navigation shortcuts.
 *
 * <p>The client sends only {@code kind/refId/tab}; routes, labels, and access
 * are derived server-side. Inaccessible or deleted targets resolve to null and
 * are quietly pruned — no error surfaces, and no client-submitted URL is ever
 * trusted. Stale entries are removed on read; a link that just became
 * inaccessible is handled by the destination page's own access error.</p>
 */
@Service
@RequiredArgsConstructor
public class RecentDestinationService {

    public static final UUID NO_REF = new UUID(0L, 0L);
    private static final Set<String> KINDS = Set.of("PROJECT", "COLLECTION", "SOURCE_LIBRARY");
    private static final int MAX_ENTRIES = 10;
    private static final int MAX_TAB_LENGTH = 50;

    private final RecentDestinationRepository recentDestinations;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final CollectionRepository collections;
    private final DocumentRepository documents;
    private final CurrentUserServiceImpl currentUsers;

    @Transactional
    public RecentDestinationResponse record(UUID userId, String kind, UUID refId, String tab) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(userId, "User"));
        String normalizedKind = kind == null ? "" : kind.trim().toUpperCase();
        if (!KINDS.contains(normalizedKind)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown destination kind");
        }
        // Empty string (never null) so the DB unique key dedupes tab-less entries.
        String normalizedTab = tab == null || tab.isBlank() ? "" : tab.trim();
        if (normalizedTab != null && normalizedTab.length() > MAX_TAB_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tab is too long");
        }
        Resolved resolved = resolve(user, normalizedKind, refId, normalizedTab);
        if (resolved == null) return null;
        UUID storedRef = refId == null ? NO_REF : refId;
        RecentDestination entry = findExisting(userId, normalizedKind, storedRef, normalizedTab)
                .orElseGet(RecentDestination::new);
        entry.setUser(user);
        entry.setKind(normalizedKind);
        entry.setRefId(storedRef);
        entry.setTab(normalizedTab);
        entry.setLabel(resolved.label());
        entry.setContext(resolved.context());
        entry.setLink(resolved.link());
        entry.setLastOpenedAt(LocalDateTime.now());
        RecentDestination saved = recentDestinations.save(entry);
        prune(userId);
        return response(saved);
    }

    @Transactional
    public List<RecentDestinationResponse> list(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(userId, "User"));
        List<RecentDestinationResponse> result = new ArrayList<>();
        for (RecentDestination entry : recentDestinations.findByUserIdOrderByLastOpenedAtDesc(userId)) {
            if (result.size() >= MAX_ENTRIES) {
                recentDestinations.delete(entry);
                continue;
            }
            UUID refId = NO_REF.equals(entry.getRefId()) ? null : entry.getRefId();
            Resolved resolved = resolve(user, entry.getKind(), refId, entry.getTab());
            if (resolved == null) {
                recentDestinations.delete(entry);
                continue;
            }
            entry.setLabel(resolved.label());
            entry.setContext(resolved.context());
            entry.setLink(resolved.link());
            result.add(response(recentDestinations.save(entry)));
        }
        return result;
    }

    private java.util.Optional<RecentDestination> findExisting(UUID userId, String kind, UUID refId, String tab) {
        return recentDestinations.findByUserIdAndKindAndRefIdAndTab(userId, kind, refId, tab)
                .stream().findFirst();
    }

    private void prune(UUID userId) {
        var entries = recentDestinations.findByUserIdOrderByLastOpenedAtDesc(userId);
        for (int i = MAX_ENTRIES; i < entries.size(); i++) {
            recentDestinations.delete(entries.get(i));
        }
    }

    private record Resolved(String label, String context, String link) {
    }

    private Resolved resolve(User user, String kind, UUID refId, String tab) {
        return switch (kind) {
            case "PROJECT" -> resolveProject(user, refId);
            case "COLLECTION" -> resolveCollection(user, refId);
            case "SOURCE_LIBRARY" -> resolveSourceLibrary(user);
            default -> null;
        };
    }

    private Resolved resolveProject(User user, UUID refId) {
        if (refId == null) return null;
        Project project = projects.findById(refId).orElse(null);
        if (project == null || !project.isActive()) return null;
        try {
            currentUsers.requireProjectAccess(user, project);
        } catch (RuntimeException denied) {
            return null;
        }
        String link = user.getRole() == UserRole.STUDENT
                ? "/student/projects/" + project.getId()
                : "/instructor/projects/" + project.getId();
        String context = project.getStatus() != null ? project.getStatus().name() : null;
        return new Resolved(project.getTitle(), context, link);
    }

    private Resolved resolveCollection(User user, UUID refId) {
        if (refId == null) return null;
        Collection collection = collections.findById(refId).orElse(null);
        if (collection == null || !collection.isActive()) return null;
        try {
            currentUsers.requireCollectionAccess(user, collection);
        } catch (RuntimeException denied) {
            return null;
        }
        long sources = documents.findByCollectionId(collection.getId()).stream()
                .filter(Document::isActive).count();
        return new Resolved(collection.getTitle(), sources + " sources",
                "/instructor/collections/" + collection.getId());
    }

    private Resolved resolveSourceLibrary(User user) {
        if (user.getRole() != UserRole.INSTRUCTOR && user.getRole() != UserRole.ADMIN) return null;
        return new Resolved(null, null, "/instructor/source-library");
    }

    private RecentDestinationResponse response(RecentDestination entry) {
        return new RecentDestinationResponse(
                entry.getKind(),
                NO_REF.equals(entry.getRefId()) ? null : entry.getRefId(),
                entry.getTab(),
                entry.getLabel(),
                entry.getContext(),
                entry.getLink(),
                entry.getLastOpenedAt());
    }
}
