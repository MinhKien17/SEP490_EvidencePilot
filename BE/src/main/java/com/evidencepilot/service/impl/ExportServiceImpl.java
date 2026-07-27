package com.evidencepilot.service.impl;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.dto.ExportRequest;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.*;
import com.evidencepilot.model.enums.ExportFormat;
import com.evidencepilot.model.enums.ExportStatus;
import com.evidencepilot.repository.*;
import com.evidencepilot.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final String EXPORT_MINIO_PREFIX = "exports/";

    private final ExportJobRepository exportJobRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final CurrentUserService currentUserService;
    private final SystemNotificationService systemNotificationService;
    private final DocumentObjectStorage documentObjectStorage;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public ExportJob createExportJob(UUID projectId, String format) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);

        ExportFormat exportFormat;
        try {
            exportFormat = ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
        }

        ExportJob job = new ExportJob();
        job.setProjectId(projectId);
        job.setUserId(currentUser.getId());
        job.setStatus(ExportStatus.PENDING);
        job.setFormat(exportFormat);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        job = exportJobRepository.save(job);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXPORT_QUEUE,
                new ExportRequest(job.getId(), projectId, currentUser.getId(), format));

        return job;
    }

    @Override
    public ExportJob getJob(UUID jobId) {
        return exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(jobId, "ExportJob"));
    }

    @Override
    public byte[] downloadExport(UUID jobId) {
        ExportJob job = getJob(jobId);
        if (job.getStatus() != ExportStatus.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Export not ready");
        }
        return documentObjectStorage.read(EXPORT_MINIO_PREFIX + jobId + ".zip");
    }

    @Override
    public List<ExportJob> getUserExports(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        return exportJobRepository.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, currentUser.getId());
    }

    public void processExport(ExportJob job) {
        job.setStatus(ExportStatus.PROCESSING);
        job.setUpdatedAt(LocalDateTime.now());
        exportJobRepository.save(job);

        try {
            byte[] archive = buildTexArchive(job.getProjectId());
            String objectKey = EXPORT_MINIO_PREFIX + job.getId() + ".zip";
            documentObjectStorage.write(objectKey, archive, "application/zip");
            String downloadUrl = documentObjectStorage.presignedGetUrl(objectKey, 60);

            job.setStatus(ExportStatus.READY);
            job.setDownloadUrl(downloadUrl);
            job.setUpdatedAt(LocalDateTime.now());
            exportJobRepository.save(job);

            User user = userRepository.getReferenceById(job.getUserId());
            systemNotificationService.createNotification(
                    user, user, "EXPORT_READY", job.getId(),
                    "Export is ready for download.");
        } catch (Exception e) {
            log.error("Export failed for job {}", job.getId(), e);
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setUpdatedAt(LocalDateTime.now());
            exportJobRepository.save(job);
        }
    }

    private byte[] buildTexArchive(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        List<Document> docs = documentRepository.findByProjectId(projectId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            zos.putNextEntry(new ZipEntry("main.tex"));
            StringBuilder mainTex = new StringBuilder();
            mainTex.append("\\documentclass{article}\n")
                    .append("\\usepackage[utf8]{inputenc}\n")
                    .append("\\usepackage{graphicx}\n")
                    .append("\\usepackage{xcolor}\n")
                    .append("\\usepackage{soul}\n")
                    .append("\\usepackage{hyperref}\n\n")
                    .append("\\title{").append(escapeLatex(
                            project.getTitle() != null ? project.getTitle() : "Untitled")).append("}\n")
                    .append("\\date{\\today}\n\n")
                    .append("\\begin{document}\n\n")
                    .append("\\maketitle\n\n");
            for (Document doc : docs) {
                String docTitle = doc.getTitle() != null ? doc.getTitle()
                        : (doc.getOriginalFilename() != null ? doc.getOriginalFilename() : "Untitled");
                mainTex.append("\\section{").append(escapeLatex(docTitle)).append("}\n\n");
                List<PaperSection> sections = paperSectionRepository
                        .findByDocumentIdOrderBySectionOrderAsc(doc.getId());
                for (PaperSection section : sections) {
                    if (section.getContentTex() != null && !section.getContentTex().isBlank()) {
                        mainTex.append("\\subsection{").append(escapeLatex(section.getSectionTitle())).append("}\n\n");
                        mainTex.append(section.getContentTex()).append("\n\n");
                    }
                }
            }
            mainTex.append("\\end{document}\n");
            zos.write(mainTex.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build export archive", e);
        }
        return baos.toByteArray();
    }

    private static String escapeLatex(String s) {
        return s.replace("\\", "\\textbackslash{}")
                .replace("&", "\\&").replace("%", "\\%")
                .replace("$", "\\$").replace("#", "\\#")
                .replace("_", "\\_").replace("{", "\\{")
                .replace("}", "\\}").replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");
    }
}
