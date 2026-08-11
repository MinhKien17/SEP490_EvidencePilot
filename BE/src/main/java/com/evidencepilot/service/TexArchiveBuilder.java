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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TexArchiveBuilder {

    private static final Pattern CITE_PATTERN = Pattern.compile(
            "\\\\cite(?:\\[[^\\]]*\\])?\\{([^}]+)\\}");
    private static final String BIBLIOGRAPHY_END = "\\end{thebibliography}";

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final PaperTexSourceBuilder paperTexSourceBuilder;
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
        Map<String, Document> activeSources = new LinkedHashMap<>();
        sourceMatchingService.activeSources(projectId)
                .forEach(source -> activeSources.put(
                        SourceMatchingService.citationKey(source.getId()), source));
        Set<String> autoCitationKeys = autoCitationKeys(sections);
        List<String> citationWarnings = new ArrayList<>();
        autoCitationKeys.stream()
                .filter(key -> !activeSources.containsKey(key))
                .forEach(key -> citationWarnings.add(
                        "Auto citation `" + key + "` no longer resolves to an active project source."));
        List<String> resolvedKeys = autoCitationKeys.stream()
                .filter(activeSources::containsKey)
                .toList();

        try (OutputStream output = Files.newOutputStream(destination);
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            StringBuilder body = new StringBuilder();
            int index = 1;
            boolean bibliographyInjected = false;
            for (PaperSection section : sections) {
                String content = section.getContentTex() == null ? "" : section.getContentTex();
                boolean referenceSection = isReferenceSection(section.getSectionTitle());
                if (referenceSection && content.isBlank() && !resolvedKeys.isEmpty()) {
                    continue;
                }
                if (referenceSection && !bibliographyInjected && !resolvedKeys.isEmpty()
                        && content.contains("\\begin{thebibliography}")
                        && content.contains(BIBLIOGRAPHY_END)) {
                    content = content.replace(
                            BIBLIOGRAPHY_END,
                            "\\input{references.tex}\n" + BIBLIOGRAPHY_END);
                    bibliographyInjected = true;
                } else if (referenceSection && !resolvedKeys.isEmpty() && !content.isBlank()) {
                    citationWarnings.add(
                            "Manual References content was preserved; generated references were appended.");
                }
                String filename = String.format(
                        "%02d-%s.tex", index++, sanitizeFilename(section.getSectionTitle()));
                body.append("\\input{sections/").append(filename).append("}\n");
                writeEntry(zip, "sections/" + filename,
                        "\\section{" + escapeLatex(section.getSectionTitle()) + "}\n\n"
                                + content + "\n");
            }
            if (!resolvedKeys.isEmpty()) {
                if (!bibliographyInjected) {
                    body.append("\\input{references.tex}\n");
                }
                writeEntry(zip, "references.tex", bibliography(
                        resolvedKeys, activeSources, !bibliographyInjected));
            }
            Document shellPaper = papers.size() == 1 ? papers.get(0) : null;
            PaperTexSourceBuilder.Shell shell = paperTexSourceBuilder.resolveShell(
                    project.getTargetStandard(),
                    project.getTitle() == null ? "Untitled" : project.getTitle(),
                    shellPaper == null ? null : shellPaper.getPreambleTex(),
                    shellPaper == null ? null : shellPaper.getFrontMatterTex());
            writeEntry(zip, "preamble.tex", shell.preambleTex() + "\n");
            writeEntry(zip, "frontmatter.tex", shell.frontMatterTex() + "\n");
            writeEntry(zip, "paper-body.tex", body.toString());
            writeEntry(zip, "main.tex", paperTexSourceBuilder.generatedMain());
            if (!citationWarnings.isEmpty()) {
                writeEntry(zip, "CITATION_WARNINGS.md", citationWarningText(citationWarnings));
            }
            mediaWriter.writeProjectMedia(projectId, zip);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build export archive", exception);
        }
    }

    private static Set<String> autoCitationKeys(List<PaperSection> sections) {
        Set<String> keys = new LinkedHashSet<>();
        for (PaperSection section : sections) {
            Matcher matcher = CITE_PATTERN.matcher(
                    section.getContentTex() == null ? "" : section.getContentTex());
            while (matcher.find()) {
                for (String key : matcher.group(1).split(",")) {
                    String trimmed = key.trim();
                    if (SourceMatchingService.citationDocumentId(trimmed).isPresent()) {
                        keys.add(trimmed);
                    }
                }
            }
        }
        return keys;
    }

    private static String bibliography(
            List<String> keys,
            Map<String, Document> sources,
            boolean includeEnvironment) {
        StringBuilder content = new StringBuilder();
        if (includeEnvironment) {
            content.append("\\begin{thebibliography}{99}\n");
        }
        for (String key : keys) {
            Document source = sources.get(key);
            String title = source.getTitle() == null || source.getTitle().isBlank()
                    ? source.getOriginalFilename() : source.getTitle();
            if (title == null || title.isBlank()) {
                title = source.getId().toString();
            }
            content.append("\\bibitem{").append(key).append("}\n");
            if (source.getAuthors() != null && !source.getAuthors().isBlank()) {
                content.append(escapeLatex(source.getAuthors())).append(". ");
            }
            content.append("\\textit{").append(escapeLatex(title)).append("}.");
            if (source.getPublisher() != null && !source.getPublisher().isBlank()) {
                content.append(' ').append(escapeLatex(source.getPublisher())).append(',');
            }
            if (source.getPublicationYear() != null) {
                content.append(' ').append(source.getPublicationYear()).append('.');
            }
            if (source.getDoi() != null && !source.getDoi().isBlank()) {
                String doi = source.getDoi().replaceFirst(
                        "(?i)^https?://(?:dx\\.)?doi\\.org/", "");
                content.append(" \\url{https://doi.org/").append(doi).append("}.");
            }
            content.append("\n\n");
        }
        if (includeEnvironment) {
            content.append(BIBLIOGRAPHY_END).append('\n');
        }
        return content.toString();
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
