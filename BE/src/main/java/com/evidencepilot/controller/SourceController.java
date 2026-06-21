package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.dto.response.SourceResponseDto;
import com.evidencepilot.dto.response.SourceChunkResponseDto;
import com.evidencepilot.repository.DatasetRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceRepository;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.SourceExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Source file uploads and CRUD operations.
 * Uploaded files are stored under the configured upload directory
 * and the resulting path is persisted to the DB.
 */
@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@Tag(name = "Sources", description = "Endpoints for managing source files and metadata")
public class SourceController {

        private final SourceRepository sourceRepository;
        private final ProjectRepository projectRepository;
        private final DatasetRepository datasetRepository;
        private final UserRepository userRepository;
        private final SourceChunkRepository sourceChunkRepository;
        private final CurrentUserService currentUserService;
        private final SourceExtractionService sourceExtractionService;

        /** Root directory where uploaded files are stored inside the container. */
        @Value("${app.upload.dir:/app/uploads}")
        private String uploadDir;

        // ── Standard CRUD ──────────────────────────────────────────────────────────

        @Operation(summary = "Get source by ID", description = "Returns metadata for a single active source by its database ID. "
                        + "Ensures access: students can only access sources within their projects; "
                        + "instructors can access dataset sources or review-state project sources; "
                        + "admins have full access. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** ADMIN")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Source metadata returned successfully"),
                        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — source access denied"),
                        @ApiResponse(responseCode = "404", description = "Source not found or inactive")
        })
        @GetMapping("/{id}")
        public SourceResponseDto findById(@PathVariable Integer id) {
                User currentUser = currentUserService.requireCurrentUser();
                Source source = sourceRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Source not found: " + id));
                if (!source.isActive()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found: " + id);
                }
                currentUserService.requireSourceAccess(currentUser, source);
                return SourceResponseDto.fromEntity(source);
        }

        @Operation(summary = "List sources by dataset", description = "Returns active sources belonging to a specific dataset. "
                        + "Ensures access: only the owner instructor of the dataset or admins can fetch this. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Sources list returned successfully"),
                        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — dataset access denied"),
                        @ApiResponse(responseCode = "404", description = "Dataset not found")
        })
        @GetMapping("/by-dataset/{datasetId}")
        public List<SourceResponseDto> findByDataset(@PathVariable Integer datasetId) {
                User currentUser = currentUserService.requireCurrentUser();
                var dataset = datasetRepository.findById(datasetId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Dataset not found: " + datasetId));
                currentUserService.requireDatasetAccess(currentUser, dataset);
                return sourceRepository.findByDatasetIdAndActiveTrue(datasetId).stream()
                                .map(SourceResponseDto::fromEntity)
                                .toList();
        }

        @Operation(summary = "Get text chunks of a source", description = "Retrieves all active text chunks and embeddings generated for the specified source file. "
                        + "Ordered sequentially by chunk index. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT, INSTRUCTOR, ADMIN")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Chunks list returned successfully"),
                        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — source access denied"),
                        @ApiResponse(responseCode = "404", description = "Source not found")
        })
        @GetMapping("/{id}/chunks")
        public List<SourceChunkResponseDto> chunks(@PathVariable Integer id) {
                User currentUser = currentUserService.requireCurrentUser();
                Source source = sourceRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Source not found: " + id));
                if (!source.isActive()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found: " + id);
                }
                currentUserService.requireSourceAccess(currentUser, source);
                return sourceChunkRepository.findBySourceId(id).stream()
                                .map(SourceChunkResponseDto::fromEntity)
                                .toList();
        }

        @Operation(summary = "Soft-delete source by ID", description = "Soft-deletes a source by setting active=false. The source is preserved in "
                        + "the database but excluded from future read mappings. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT, INSTRUCTOR, ADMIN")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Source soft-deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — source write access denied"),
                        @ApiResponse(responseCode = "404", description = "Source not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Integer id) {
                User currentUser = currentUserService.requireCurrentUser();
                Source source = sourceRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Source not found: " + id));
                if (source.getProject() != null) {
                        currentUserService.requireProjectWriteAccess(currentUser, source.getProject());
                } else if (source.getDataset() != null) {
                        currentUserService.requireDatasetAccess(currentUser, source.getDataset());
                } else {
                        currentUserService.requireSourceAccess(currentUser, source);
                }
                source.setActive(false);
                sourceRepository.save(source);
                return ResponseEntity.noContent().build();
        }

        // ── File upload ────────────────────────────────────────────────────────────

        /**
         * Uploads a source file for a project or dataset.
         *
         * <p>
         * The file is stored at {@code <uploadDir>/sources/<uuid>_<originalName>}
         * and the path is saved as {@code file_url} in the DB. No hashing or
         * deduplication is performed.
         * </p>
         *
         * @param file         the multipart file
         * @param uploadedById ID of the user uploading the file (required)
         * @param projectId    optional – if present the source is linked to this
         *                     project
         * @param datasetId    optional – if present the source is linked to this
         *                     dataset
         */
        @Operation(summary = "Upload a source file", description = "Accepts a file upload (multipart/form-data) and links it to a project or dataset. "
                        + "Saves the file to the server filesystem, registers it in the DB, and triggers "
                        + "text extraction asynchronously. "
                        + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** STUDENT, INSTRUCTOR, ADMIN")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Source uploaded and created successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad Request — invalid file, parameter type, or directory access"),
                        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — insufficient write permissions"),
                        @ApiResponse(responseCode = "404", description = "Associated user, project, or dataset not found")
        })
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Transactional
        public ResponseEntity<SourceResponseDto> upload(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("uploadedBy") Integer uploadedById,
                        @RequestParam(value = "projectId", required = false) Integer projectId,
                        @RequestParam(value = "datasetId", required = false) Integer datasetId) {

                User currentUser = currentUserService.requireCurrentUser();
                currentUserService.requireUserIdOrAdmin(currentUser, uploadedById);

                // ── Resolve relations ──────────────────────────────────────────────────
                User uploader = userRepository.findById(uploadedById)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "User not found: " + uploadedById));

                Source source = new Source();
                source.setUploadedBy(uploader);
                source.setOriginalFilename(safeOriginalFilename(file));
                source.setContentType(file.getContentType());
                source.setFileSizeBytes(file.getSize());

                if (projectId != null) {
                        var project = projectRepository.findById(projectId)
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Project not found: " + projectId));
                        currentUserService.requireProjectWriteAccess(currentUser, project);
                        source.setProject(project);
                }
                if (datasetId != null) {
                        var dataset = datasetRepository.findById(datasetId)
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Dataset not found: " + datasetId));
                        currentUserService.requireDatasetAccess(currentUser, dataset);
                        source.setDataset(dataset);
                }

                // ── Persist the file ───────────────────────────────────────────────────
                String savedPath = storeFile(file, "sources");
                source.setFileUrl(savedPath);

                Source saved = sourceRepository.save(source);
                sourceExtractionService.extractAndPersist(saved, file);
                return ResponseEntity.status(HttpStatus.CREATED).body(SourceResponseDto.fromEntity(saved));
        }

        // ── Internal helpers ───────────────────────────────────────────────────────

        /**
         * Writes the multipart file to {@code <uploadDir>/<subDir>/<uuid>_<original>}
         * and returns the resulting absolute path string.
         */
        private String storeFile(MultipartFile file, String subDir) {
                try {
                        Path directory = Paths.get(uploadDir, subDir);
                        Files.createDirectories(directory);

                        String filename = UUID.randomUUID() + "_" + safeOriginalFilename(file);
                        Path destination = directory.resolve(filename).normalize();
                        if (!destination.startsWith(directory)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Invalid file path detected");
                        }
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
                        return "uploaded-source";
                }
                String filename = Paths.get(original).getFileName().toString();
                return filename.replaceAll("[^A-Za-z0-9._-]", "_");
        }
}
