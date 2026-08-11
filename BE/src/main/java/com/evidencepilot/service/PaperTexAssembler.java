package com.evidencepilot.service;

import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaperTexAssembler {

    public static final int MAX_SOURCE_BYTES = 2 * 1024 * 1024;
    private static final Pattern UNSAFE_PRIMITIVE = Pattern.compile(
            "(?i)\\\\(?:write18|openin|openout)\\b");
    private static final Pattern FILE_COMMAND = Pattern.compile(
            "(?i)\\\\(?:input|include)\\s*\\{([^}]*)}");
    private static final Pattern IMAGE_COMMAND = Pattern.compile(
            "(?i)\\\\includegraphics(?:\\s*\\[[^]]*])?\\s*\\{([^}]*)}");

    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final CurrentUserService currentUserService;
    private final PaperTexSourceBuilder sourceBuilder;
    private final TexArchiveMediaWriter mediaWriter;

    public PaperTexShell getShell(UUID documentId) {
        Document paper = requirePaper(documentId, false);
        PaperTexSourceBuilder.Shell shell = resolvedShell(paper, null);
        return new PaperTexShell(shell.preambleTex(), shell.frontMatterTex(), paper.getVersion());
    }

    @Transactional
    public PaperTexShell updateShell(
            UUID documentId,
            String preambleTex,
            String frontMatterTex,
            Long version) {
        Document paper = requirePaper(documentId, true);
        if (!Objects.equals(paper.getVersion(), version)) {
            throw new ObjectOptimisticLockingFailureException(Document.class, documentId);
        }
        validateFragment("preamble.tex", preambleTex);
        validateFragment("frontmatter.tex", frontMatterTex);
        if (preambleTex.contains("\\begin{document}") || preambleTex.contains("\\end{document}")
                || frontMatterTex.contains("\\begin{document}")
                || frontMatterTex.contains("\\end{document}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "preamble.tex and frontmatter.tex must not contain document boundaries");
        }
        paper.setPreambleTex(preambleTex.strip());
        paper.setFrontMatterTex(frontMatterTex.strip());
        Document saved = documentRepository.saveAndFlush(paper);
        return new PaperTexShell(saved.getPreambleTex(), saved.getFrontMatterTex(), saved.getVersion());
    }

    public PaperTexWorkspace assemble(UUID documentId, DraftOverride override) {
        Document paper = requirePaper(documentId, false);
        List<PaperSection> sections = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(documentId).stream()
                .filter(PaperSection::isActive)
                .toList();
        User user = currentUserService.requireCurrentUser();
        PaperTexSourceBuilder.Shell shell = resolvedShell(paper, override);

        SectionDraft sectionDraft = override == null ? null : override.sectionOverride();
        if (sectionDraft != null) {
            PaperSection section = paperSectionRepository.findByIdWithDocument(sectionDraft.sectionId())
                    .filter(candidate -> candidate.getDocument() != null
                            && documentId.equals(candidate.getDocument().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException(sectionDraft.sectionId(), "PaperSection"));
            currentUserService.requireSectionContentWriteAccess(user, section);
        }

        Path root;
        try {
            root = Files.createTempDirectory("evidencepilot-tex-preview-");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create TeX workspace", exception);
        }

        PaperTexWorkspace workspace = new PaperTexWorkspace(root);
        try {
            StringBuilder body = new StringBuilder();
            int sourceBytes = 0;
            int order = 1;
            for (PaperSection section : sections) {
                String content = sectionDraft != null && section.getId().equals(sectionDraft.sectionId())
                        ? sectionDraft.contentTex()
                        : Objects.requireNonNullElse(section.getContentTex(), "");
                validateFragment("section " + section.getId(), content);
                String filename = String.format(
                        "sections/%02d-%s.tex",
                        order++,
                        section.getId() == null ? UUID.randomUUID() : section.getId());
                String sectionTex = "\\section{"
                        + PaperTexSourceBuilder.escapeLatex(section.getSectionTitle())
                        + "}\n\n" + content.strip() + "\n";
                sourceBytes += sectionTex.getBytes(StandardCharsets.UTF_8).length;
                write(root, filename, sectionTex);
                body.append("\\input{").append(filename).append("}\n");
            }

            validateFragment("preamble.tex", shell.preambleTex());
            validateFragment("frontmatter.tex", shell.frontMatterTex());
            sourceBytes += shell.preambleTex().getBytes(StandardCharsets.UTF_8).length
                    + shell.frontMatterTex().getBytes(StandardCharsets.UTF_8).length
                    + body.toString().getBytes(StandardCharsets.UTF_8).length
                    + sourceBuilder.generatedMain().getBytes(StandardCharsets.UTF_8).length;
            if (sourceBytes > MAX_SOURCE_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "TeX source exceeds the 2 MiB preview limit");
            }

            write(root, "preamble.tex", shell.preambleTex() + "\n");
            write(root, "frontmatter.tex", shell.frontMatterTex() + "\n");
            write(root, "paper-body.tex", body.toString());
            write(root, "main.tex", sourceBuilder.generatedMain());
            mediaWriter.writeProjectMedia(paper.getProject().getId(), root);
            requireAvailableImages(root, shell.preambleTex());
            requireAvailableImages(root, shell.frontMatterTex());
            for (PaperSection section : sections) {
                String content = sectionDraft != null && section.getId().equals(sectionDraft.sectionId())
                        ? sectionDraft.contentTex()
                        : Objects.requireNonNullElse(section.getContentTex(), "");
                requireAvailableImages(root, content);
            }
            return workspace;
        } catch (IOException | RuntimeException exception) {
            workspace.close();
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to assemble TeX workspace", exception);
        }
    }

    private PaperTexSourceBuilder.Shell resolvedShell(Document paper, DraftOverride override) {
        String preamble = paper.getPreambleTex();
        String frontMatter = paper.getFrontMatterTex();
        ShellDraft shellDraft = override == null ? null : override.shellOverride();
        if (shellDraft != null) {
            User user = currentUserService.requireCurrentUser();
            currentUserService.requireProjectWriteAccess(user, paper.getProject());
            if (shellDraft.kind() == ShellKind.PREAMBLE) {
                preamble = shellDraft.contentTex();
            } else {
                frontMatter = shellDraft.contentTex();
            }
        }
        return sourceBuilder.resolveShell(
                paper.getProject().getTargetStandard(), paperTitle(paper), preamble, frontMatter);
    }

    private Document requirePaper(UUID documentId, boolean write) {
        Document paper = documentRepository.findById(documentId)
                .filter(document -> document.getDocType() == DocumentType.PAPER && document.isActive())
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Paper"));
        if (paper.getProject() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paper is not attached to a project");
        }
        User user = currentUserService.requireCurrentUser();
        if (write) {
            currentUserService.requireProjectWriteAccess(user, paper.getProject());
        } else {
            currentUserService.requireProjectAccess(user, paper.getProject());
        }
        return paper;
    }

    private static String paperTitle(Document paper) {
        if (paper.getTitle() != null && !paper.getTitle().isBlank()) {
            return paper.getTitle();
        }
        if (paper.getOriginalFilename() != null && !paper.getOriginalFilename().isBlank()) {
            return paper.getOriginalFilename();
        }
        return paper.getProject().getTitle() == null ? "Untitled" : paper.getProject().getTitle();
    }

    private static void validateFragment(String name, String content) {
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " is required");
        }
        if (UNSAFE_PRIMITIVE.matcher(content).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    name + " contains a disabled TeX primitive");
        }
        Matcher matcher = FILE_COMMAND.matcher(content);
        while (matcher.find()) {
            if (unsafePath(matcher.group(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        name + " contains an unsafe file path");
            }
        }
        matcher = IMAGE_COMMAND.matcher(content);
        while (matcher.find()) {
            if (!ExtractionBundle.validTexImagePath(matcher.group(1).strip())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        name + " contains an unsafe image path");
            }
        }
    }

    private static boolean unsafePath(String path) {
        String value = path == null ? "" : path.strip();
        return value.isBlank() || value.startsWith("/") || value.startsWith("\\")
                || value.contains("..") || value.contains(":");
    }

    private static void requireAvailableImages(Path root, String content) {
        Matcher matcher = IMAGE_COMMAND.matcher(content);
        while (matcher.find()) {
            String relative = matcher.group(1).strip();
            Path image = root.resolve(relative).normalize();
            if (!image.startsWith(root) || !Files.isRegularFile(image)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "TeX references an image that is not in project media: " + relative);
            }
        }
    }

    private static void write(Path root, String relative, String content) throws IOException {
        Path destination = root.resolve(relative).normalize();
        if (!destination.startsWith(root)) {
            throw new IOException("Unsafe TeX workspace path");
        }
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, content, StandardCharsets.UTF_8);
    }

    public record PaperTexShell(String preambleTex, String frontMatterTex, Long version) {
    }

    public record DraftOverride(SectionDraft sectionOverride, ShellDraft shellOverride) {
    }

    public record SectionDraft(UUID sectionId, String contentTex) {
    }

    public record ShellDraft(ShellKind kind, String contentTex) {
    }

    public enum ShellKind {
        PREAMBLE,
        FRONT_MATTER
    }

    public static final class PaperTexWorkspace implements AutoCloseable {
        private final Path root;

        PaperTexWorkspace(Path root) {
            this.root = root;
        }

        public Path root() {
            return root;
        }

        public Path mainTex() {
            return root.resolve("main.tex");
        }

        @Override
        public void close() {
            if (root == null || !Files.exists(root)) {
                return;
            }
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
        }
    }
}
