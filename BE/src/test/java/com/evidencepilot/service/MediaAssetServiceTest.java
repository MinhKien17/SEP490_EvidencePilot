package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.ProjectMediaRepository;
import com.evidencepilot.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaAssetServiceTest {

    @Mock
    private ProjectMediaRepository projectMediaRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private DocumentObjectStorage objectStorage;
    @Mock
    private CurrentUserService currentUserService;

    @Test
    void importExtractedImageUsesStableStorageKeyAndTexPath() {
        Document source = sourceDocument();
        String texFilename = "images/figure.jpg";
        String storageKey = "media/" + source.getProject().getId()
                + "/extracted/" + source.getId() + "/" + texFilename;
        when(projectMediaRepository.existsByProjectIdAndStorageKey(
                source.getProject().getId(), storageKey)).thenReturn(false);

        service().importExtractedImage(
                source,
                texFilename,
                new ByteArrayInputStream(new byte[] {1, 2, 3}),
                3,
                "image/jpeg");

        verify(objectStorage).write(
                eq(storageKey),
                any(InputStream.class),
                eq(3L),
                eq("image/jpeg"));
        verify(projectMediaRepository).save(argThat(media ->
                media.getProject() == source.getProject()
                        && media.getUploadedBy() == source.getUploadedBy()
                        && media.getStorageKey().equals(storageKey)
                        && media.getTexFilename().equals(texFilename)));
    }

    @Test
    void importExtractedImageSkipsAnExistingStorageKey() {
        Document source = sourceDocument();
        String texFilename = "images/figure.jpg";
        String storageKey = "media/" + source.getProject().getId()
                + "/extracted/" + source.getId() + "/" + texFilename;
        when(projectMediaRepository.existsByProjectIdAndStorageKey(
                source.getProject().getId(), storageKey)).thenReturn(true);

        service().importExtractedImage(
                source,
                texFilename,
                new ByteArrayInputStream(new byte[] {1, 2, 3}),
                3,
                "image/jpeg");

        verify(objectStorage, never()).write(any(), any(InputStream.class), any(Long.class), any());
        verify(projectMediaRepository, never()).save(any());
    }

    private MediaAssetService service() {
        return new MediaAssetService(
                projectMediaRepository, projectRepository, objectStorage, currentUserService);
    }

    private static Document sourceDocument() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        User user = new User();
        user.setId(UUID.randomUUID());
        Document source = new Document();
        source.setId(UUID.randomUUID());
        source.setProject(project);
        source.setUploadedBy(user);
        return source;
    }
}
