package com.evidencepilot.service;

import com.evidencepilot.dto.response.ClaimConsistencyResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TexArchiveBuilder {

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final PaperStandardService paperStandardService;
    private final ClaimContentConsistencyService consistencyService;
    private final TexArchiveMediaWriter mediaWriter;

    public Path build(UUID projectId) {
        Path destination;
        try {
            destination = Files.createTempFile("evidencepilot-project-export-", ".zip");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create export archive", exception);
        }
        try {
            write(projectId, destination);
            return destination;
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    public void write(UUID projectId, Path destination) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        List<Document> papers = documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(projectId, DocumentType.PAPER);
        ClaimConsistencyResponse preflight = consistencyService.preflight(projectId);

        try (OutputStream output = Files.newOutputStream(destination);
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            StringBuilder body = new StringBuilder();
            int index = 1;
            for (Document paper : papers) {
                for (PaperSection section : paperSectionRepository
                        .findByDocumentIdOrderBySectionOrderAsc(paper.getId())) {
                    if (!section.isActive()) continue;
                    String filename = String.format(
                            "%02d-%s.tex", index++, sanitizeFilename(section.getSectionTitle()));
                    body.append("\\input{sections/").append(filename).append("}\n");
                    writeEntry(zip, "sections/" + filename,
                            "\\section{" + escapeLatex(section.getSectionTitle()) + "}\n\n"
                                    + (section.getContentTex() == null
                                            ? "" : section.getContentTex())
                                    + "\n");
                }
            }
            writeEntry(zip, "main.tex", paperStandardService.renderTemplate(
                    project.getTargetStandard(),
                    escapeLatex(project.getTitle() == null ? "Untitled" : project.getTitle()),
                    body.toString()));
            if (preflight.warningCount() > 0) {
                writeEntry(zip, "CLAIM_WARNINGS.md", warningText(preflight));
            }
            mediaWriter.writeProjectMedia(projectId, zip);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build export archive", exception);
        }
    }

    private static String warningText(ClaimConsistencyResponse preflight) {
        StringBuilder content = new StringBuilder(
                "# Claim usage warnings\n\n"
                + "The paper was exported, but the following stored Claims are not "
                + "consistently represented in their owning Sections:\n\n");
        for (ClaimConsistencyResponse.Warning warning : preflight.warnings()) {
            content.append("- `").append(warning.claimId()).append("` — ")
                    .append(warning.status()).append(": ")
                    .append(warning.message()).append('\n');
        }
        return content.toString();
    }

    private static void writeEntry(ZipOutputStream zip, String name, String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String escapeLatex(String value) {
        return value.replace("\\", "\\textbackslash{}")
                .replace("&", "\\&").replace("%", "\\%")
                .replace("$", "\\$").replace("#", "\\#")
                .replace("_", "\\_").replace("{", "\\{")
                .replace("}", "\\}").replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");
    }

    private static String sanitizeFilename(String value) {
        String filename = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return filename.isBlank() ? "untitled" : filename;
    }
}
