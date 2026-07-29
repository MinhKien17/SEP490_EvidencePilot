package com.evidencepilot.service;

import com.evidencepilot.dto.response.ProjectMediaResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMedia;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.ProjectMediaRepository;
import com.evidencepilot.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaAssetService {

    private final ProjectMediaRepository projectMediaRepository;
    private final ProjectRepository projectRepository;
    private final DocumentObjectStorage objectStorage;
    private final CurrentUserService currentUserService;

    @Transactional
    public ProjectMediaResponse upload(MultipartFile file, UUID projectId) {
        User user = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        currentUserService.requireProjectWriteAccess(user, project);

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is required");
        }

        String texFilename = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storageKey = "media/" + projectId + "/" + UUID.randomUUID() + "-" + texFilename;

        try (var in = file.getInputStream()) {
            objectStorage.write(storageKey, in, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload to storage", e);
        }

        ProjectMedia media = new ProjectMedia();
        media.setProject(project);
        media.setUploadedBy(user);
        media.setStorageKey(storageKey);
        media.setTexFilename(texFilename);
        media.setMimeType(file.getContentType());
        media.setUploadedAt(LocalDateTime.now());
        media = projectMediaRepository.save(media);

        return toResponse(media);
    }

    public List<ProjectMediaResponse> listByProject(UUID projectId) {
        return projectMediaRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectMedia getMedia(UUID id) {
        return projectMediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
    }

    @Transactional
    public void importExtractedImage(
            Document source,
            String texFilename,
            InputStream content,
            long size,
            String mimeType) {
        Project project = source.getProject();
        if (project == null) {
            return;
        }
        String storageKey = "media/" + project.getId()
                + "/extracted/" + source.getId() + "/" + texFilename;
        if (projectMediaRepository.existsByProjectIdAndStorageKey(project.getId(), storageKey)) {
            return;
        }

        objectStorage.write(storageKey, content, size, mimeType);

        ProjectMedia media = new ProjectMedia();
        media.setProject(project);
        media.setUploadedBy(source.getUploadedBy());
        media.setStorageKey(storageKey);
        media.setTexFilename(texFilename);
        media.setMimeType(mimeType);
        media.setUploadedAt(LocalDateTime.now());
        projectMediaRepository.save(media);
    }

    public String getSignedUrl(UUID id) {
        ProjectMedia media = getMedia(id);
        return objectStorage.presignedGetUrl(media.getStorageKey(), 60);
    }

    @Transactional
    public void delete(UUID id) {
        User user = currentUserService.requireCurrentUser();
        ProjectMedia media = projectMediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        currentUserService.requireProjectWriteAccess(user, media.getProject());
        projectMediaRepository.delete(media);
    }

    private ProjectMediaResponse toResponse(ProjectMedia m) {
        return new ProjectMediaResponse(
                m.getId(),
                m.getProject().getId(),
                m.getUploadedBy().getId(),
                m.getStorageKey(),
                m.getTexFilename(),
                m.getMimeType(),
                m.getUploadedAt()
        );
    }
}
