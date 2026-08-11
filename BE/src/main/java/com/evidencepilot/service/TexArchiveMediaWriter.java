package com.evidencepilot.service;

import com.evidencepilot.model.ProjectMedia;
import com.evidencepilot.repository.ProjectMediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
public class TexArchiveMediaWriter {

    private final ProjectMediaRepository projectMediaRepository;
    private final DocumentObjectStorage objectStorage;

    public void writeProjectMedia(UUID projectId, ZipOutputStream archive) throws IOException {
        Set<String> written = new HashSet<>();
        for (ProjectMedia media : projectMediaRepository.findByProjectIdOrderByIdAsc(projectId)) {
            String path = media.getTexFilename();
            if (!ExtractionBundle.validTexImagePath(path) || !written.add(path)) {
                continue;
            }
            archive.putNextEntry(new ZipEntry(path));
            try (InputStream content = objectStorage.getStream(media.getStorageKey())) {
                content.transferTo(archive);
            }
            archive.closeEntry();
        }
    }

    public void writeProjectMedia(UUID projectId, Path workspace) throws IOException {
        Path root = workspace.toAbsolutePath().normalize();
        Set<String> written = new HashSet<>();
        for (ProjectMedia media : projectMediaRepository.findByProjectIdOrderByIdAsc(projectId)) {
            String path = media.getTexFilename();
            if (!ExtractionBundle.validTexImagePath(path) || !written.add(path)) {
                continue;
            }
            Path destination = root.resolve(path).normalize();
            if (!destination.startsWith(root)) {
                continue;
            }
            Files.createDirectories(destination.getParent());
            try (InputStream content = objectStorage.getStream(media.getStorageKey());
                    var output = Files.newOutputStream(destination)) {
                content.transferTo(output);
            }
        }
    }
}
