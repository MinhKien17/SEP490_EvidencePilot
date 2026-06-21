package com.evidencepilot.controller;

import com.evidencepilot.ai.dto.PaperReviewResponse;
import com.evidencepilot.domain.entity.Paper;
import com.evidencepilot.domain.entity.PaperSection;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.response.PaperResponseDto;
import com.evidencepilot.dto.response.PaperSectionResponseDto;
import com.evidencepilot.repository.PaperRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.PaperProcessingService;
import com.evidencepilot.service.SourceExtractionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Paper file uploads and CRUD operations.
 * Uploaded files are stored under the configured upload directory
 * and the resulting path is persisted to the DB.
 *
 * Base path: /api/papers
 */
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperRepository paperRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final SourceExtractionService sourceExtractionService;
    private final PaperProcessingService paperProcessingService;

    /** Root directory where uploaded files are stored inside the container. */
    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    // ── Standard CRUD ──────────────────────────────────────────────────────────

    @GetMapping
    public List<PaperResponseDto> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        List<Paper> papers;
        if (currentUserService.isAdmin(currentUser)) {
            papers = paperRepository.findByActiveTrue();
        } else {
            papers = paperRepository.findByProjectStudentIdAndActiveTrue(currentUser.getId());
        }
        return papers.stream().map(PaperResponseDto::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public PaperResponseDto findById(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        if (!paper.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        currentUserService.requireProjectWriteAccess(currentUser, paper.getProject());
        return PaperResponseDto.fromEntity(paper);
    }

    @GetMapping("/by-project/{projectId}")
    public List<PaperResponseDto> findByProject(@PathVariable Integer projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectWriteAccess(currentUser, project);
        return paperRepository.findByProjectIdAndActiveTrue(projectId).stream()
                .map(PaperResponseDto::fromEntity)
                .toList();
    }

    @GetMapping("/{id}/sections")
    public List<PaperSectionResponseDto> sections(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        if (!paper.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        currentUserService.requirePaperAccess(currentUser, paper);
        return paperSectionRepository.findByPaperIdOrderBySectionIndex(id).stream()
                .map(PaperSectionResponseDto::fromEntity)
                .toList();
    }

    @PostMapping("/{id}/review")
    public PaperReviewResponse review(
            @PathVariable Integer id,
            @RequestParam(required = false) String targetStyle) {

        User currentUser = currentUserService.requireCurrentUser();
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        if (!paper.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        currentUserService.requirePaperAccess(currentUser, paper);
        return paperProcessingService.review(paper, targetStyle);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        currentUserService.requirePaperAccess(currentUser, paper);
        paper.setActive(false);
        paperRepository.save(paper);
        return ResponseEntity.noContent().build();
    }

    // ── File upload ────────────────────────────────────────────────────────────

    /**
     * Uploads a paper (PDF/document) and links it to a project.
     *
     * <p>The file is stored at {@code <uploadDir>/papers/<uuid>_<originalName>}
     * and the path is saved as {@code file_url} in the DB.  No hashing or
     * deduplication is performed.</p>
     *
     * @param file      the multipart file
     * @param projectId the project this paper belongs to (required)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<PaperResponseDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Integer projectId) {

        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectAccess(currentUser, project);

        SourceExtractionService.ExtractedText extracted = sourceExtractionService.extractText(file);
        String savedPath = storeFile(file, "papers");

        Paper paper = new Paper();
        paper.setProject(project);
        paper.setFileUrl(savedPath);
        paper.setOriginalFilename(safeOriginalFilename(file));
        paper.setContentType(file.getContentType());
        paper.setFileSizeBytes(file.getSize());
        paper.setExtractedText(extracted.text());
        paper.setExtractionMethod(extracted.method());
        paper.setSubmittedAt(LocalDateTime.now());

        Paper saved = paperRepository.save(paper);
        paperProcessingService.detectAndPersistSections(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaperResponseDto.fromEntity(saved));
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private String storeFile(MultipartFile file, String subDir) {
        try {
            Path directory = Paths.get(uploadDir, subDir);
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + "_" + safeOriginalFilename(file);
            Path destination = directory.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return destination.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + e.getMessage(), e);
        }
    }

    private String safeOriginalFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "uploaded-paper";
        }
        String filename = Paths.get(original).getFileName().toString();
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
