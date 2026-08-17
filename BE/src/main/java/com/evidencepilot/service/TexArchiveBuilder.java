package com.evidencepilot.service;

import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.impl.SourceMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
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
    private final TexArchiveMediaWriter mediaWriter;
    private final SourceMatchingService sourceMatchingService;

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
        List<PaperSection> sections = papers.stream()
                .flatMap(paper -> paperSectionRepository
                        .findByDocumentIdOrderBySectionOrderAsc(paper.getId()).stream())
                .filter(PaperSection::isActive)
                .toList();
        CitationBibliography.Result bibliography = CitationBibliography.resolve(
                sections, sourceMatchingService.activeSources(projectId));
        List<String> citationWarnings = new ArrayList<>();
        bibliography.unresolvedKeys().forEach(key -> citationWarnings.add(
                        "Auto citation `" + key + "` no longer resolves to an active project source."));

        try (OutputStream output = Files.newOutputStream(destination);
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            StringBuilder body = new StringBuilder();
            int index = 1;
            boolean bibliographyInjected = false;
            for (PaperSection section : sections) {
                String content = section.getContentTex() == null ? "" : section.getContentTex();
                boolean referenceSection = isReferenceSection(section.getSectionTitle());
                if (referenceSection && content.isBlank() && !bibliography.entries().isEmpty()) {
                    continue;
                }
                if (referenceSection && !bibliographyInjected && !bibliography.entries().isEmpty()
                        && content.contains("\\begin{thebibliography}")
                        && content.contains(CitationBibliography.BIBLIOGRAPHY_END)) {
                    content = content.replace(
                            CitationBibliography.BIBLIOGRAPHY_END,
                            "\\input{references.tex}\n" + CitationBibliography.BIBLIOGRAPHY_END);
                    bibliographyInjected = true;
                } else if (referenceSection && !bibliography.entries().isEmpty() && !content.isBlank()) {
                    citationWarnings.add(
                            "Manual References content was preserved; generated references were appended.");
                }
                String filename = String.format(
                        "%02d-%s.tex", index++, sanitizeFilename(section.getSectionTitle()));
                body.append("\\input{sections/").append(filename).append("}\n");
                writeEntry(zip, "sections/" + filename,
                        "\\section{" + CitationBibliography.escapeLatex(section.getSectionTitle()) + "}\n\n"
                                + content + "\n");
            }
            if (!bibliography.entries().isEmpty()) {
                if (!bibliographyInjected) {
                    body.append("\\input{references.tex}\n");
                }
                writeEntry(zip, "references.tex", bibliography.toLatex(!bibliographyInjected));
            }
            writeEntry(zip, "main.tex", paperStandardService.renderTemplate(
                    project.getTargetStandard(),
                    CitationBibliography.escapeLatex(
                            project.getTitle() == null ? "Untitled" : project.getTitle()),
                    body.toString()));
            if (!citationWarnings.isEmpty()) {
                writeEntry(zip, "CITATION_WARNINGS.md", citationWarningText(citationWarnings));
            }
            mediaWriter.writeProjectMedia(projectId, zip);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build export archive", exception);
        }
    }

    private static String citationWarningText(List<String> warnings) {
        StringBuilder content = new StringBuilder("# Citation export warnings\n\n");
        warnings.forEach(warning -> content.append("- ").append(warning).append('\n'));
        return content.toString();
    }

    private static boolean isReferenceSection(String title) {
        String normalized = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("references")
                || normalized.equals("reference")
                || normalized.equals("bibliography")
                || normalized.equals("works cited");
    }

    private static void writeEntry(ZipOutputStream zip, String name, String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String sanitizeFilename(String value) {
        String filename = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return filename.isBlank() ? "untitled" : filename;
    }
}
