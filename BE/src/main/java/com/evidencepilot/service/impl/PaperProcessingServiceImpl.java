package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.PaperValidationResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SectionFeedbackRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.PaperProcessingService;
import com.evidencepilot.service.PaperStandardService;
import com.evidencepilot.service.SystemNotificationService;
import com.evidencepilot.service.TexArchiveMediaWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperProcessingServiceImpl implements PaperProcessingService {

    private static final int REVIEW_TEXT_LIMIT = 10_000;

    private final AiModelClient aiModelClient;
    private final PaperSectionRepository paperSectionRepository;
    private final SectionFeedbackRepository sectionFeedbackRepository;
    private final DocumentRepository documentRepository;
    private final ProjectMapper projectMapper;
    private final CurrentUserService currentUserService;
    private final PaperStandardService paperStandardService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final SystemNotificationService systemNotificationService;
    private final TexArchiveMediaWriter texArchiveMediaWriter;

    @Override
    public List<PaperSectionResponse> getPaperSections(UUID documentId) {
        requireDocumentAccess(documentId);
        return paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(documentId).stream()
                .map(projectMapper::toPaperSectionResponse)
                .toList();
    }

    @Override
    public List<PaperSectionResponse> getPaperSectionsByUser(UUID documentId, UUID userId) {
        requireDocumentAccess(documentId);
        return paperSectionRepository
                .findByDocumentIdAndAssignedUserIdOrderBySectionOrderAsc(documentId, userId)
                .stream()
                .map(projectMapper::toPaperSectionResponse)
                .toList();
    }

    @Override
    public PaperSectionResponse getSectionHistory(UUID documentId, UUID sectionId) {
        requireDocumentAccess(documentId);
        PaperSection section = requireSectionInDocument(sectionId, documentId);
        return projectMapper.toPaperSectionResponse(section);
    }

    @Override
    @Transactional
    public List<PaperSectionResponse> detectAndPersistSections(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
        List<PaperSection> existing = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(documentId);
        if (!existing.isEmpty()) {
            return existing.stream()
                    .map(projectMapper::toPaperSectionResponse)
                    .toList();
        }
        String text = document.getDocumentText() != null
                ? document.getDocumentText().getExtractedText() : null;
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<PaperSection> sections = parseSections(text, document);
        return paperSectionRepository.saveAll(sections).stream()
                .map(projectMapper::toPaperSectionResponse)
                .toList();
    }

    @Override
    public Map<String, Object> review(UUID documentId, String targetStyle) {
        Document document = requireDocumentAccess(documentId);
        String style = targetStyle != null ? targetStyle : "default";
        String text = document.getDocumentText() != null
                ? document.getDocumentText().getExtractedText() : "";
        String prompt = "Review this paper for target style: " + style
                + ". Return concise, actionable feedback.\n\n"
                + text.substring(0, Math.min(text.length(), REVIEW_TEXT_LIMIT));
        try {
            String review = aiModelClient.generate(prompt);
            return Map.of(
                    "paper_id", document.getId().toString(),
                    "target_style", style,
                    "review", review);
        } catch (AiModelClient.AiApiException e) {
            log.error("Paper review failed for document {}: {}", document.getId(), e.getMessage());
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Paper review service unavailable", e);
        }
    }

    @Override
    public PaperValidationResponse validateSections(UUID documentId) {
        Document document = requireDocumentAccess(documentId);
        Project project = document.getProject();
        if (project == null || project.getTargetStandard() == null) {
            return new PaperValidationResponse(true, List.of(), List.of(), List.of(), null);
        }

        PaperStandard standard = project.getTargetStandard();
        List<String> required = paperStandardService.getRequiredSections(standard);
        if (required.isEmpty()) {
            return new PaperValidationResponse(true, List.of(), List.of(), List.of(), standard);
        }

        List<PaperSection> sections = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(documentId);
        List<String> actualTitles = sections.stream()
                .map(s -> paperStandardService.normalizeSectionTitle(s.getSectionTitle()))
                .toList();

        List<String> missing = new ArrayList<>(required);
        missing.removeAll(actualTitles);

        List<String> extra = new ArrayList<>(actualTitles);
        extra.removeAll(required);

        LinkedHashSet<String> ordered = new LinkedHashSet<>(actualTitles);
        ordered.retainAll(required);
        List<String> orderedList = new ArrayList<>(ordered);
        List<String> expectedOrder = required.stream()
                .filter(orderedList::contains)
                .toList();
        List<String> outOfOrder = new ArrayList<>();
        for (int i = 0; i < orderedList.size() && i < expectedOrder.size(); i++) {
            if (!orderedList.get(i).equals(expectedOrder.get(i))) {
                outOfOrder.add(orderedList.get(i));
            }
        }

        boolean valid = missing.isEmpty() && extra.isEmpty() && outOfOrder.isEmpty();
        return new PaperValidationResponse(valid, missing, extra, outOfOrder, standard);
    }

    @Override
    @Transactional
    public PaperSectionResponse updateSection(UUID documentId, UUID sectionId,
            String title, Integer order, UUID mergeIntoId, String content) {
        requireDocumentWriteAccess(documentId);
        User currentUser = currentUserService.requireCurrentUser();

        if (mergeIntoId != null) {
            PaperSection target = requireSectionInDocument(mergeIntoId, documentId);
            PaperSection source = requireSectionInDocument(sectionId, documentId);
            currentUserService.requireSectionAssignment(currentUser, source);
            currentUserService.requireSectionAssignment(currentUser, target);
            target.setContentTex(
                    (target.getContentTex() != null ? target.getContentTex() : "")
                    + "\n\n" + (source.getContentTex() != null ? source.getContentTex() : ""));
            target.setContentMdCache(null);
            target.setUpdatedAt(LocalDateTime.now());
            paperSectionRepository.save(target);
            source.setActive(false);
            paperSectionRepository.save(source);
            return projectMapper.toPaperSectionResponse(target);
        }

        PaperSection section = requireSectionInDocument(sectionId, documentId);
        currentUserService.requireSectionAssignment(currentUser, section);
        if (title != null && !title.isBlank()) {
            section.setSectionTitle(title);
        }
        if (order != null) {
            section.setSectionOrder(order);
        }
        if (content != null) {
            section.setPreviousContentTex(section.getContentTex());
            section.setContentTex(content);
            // ponytail: cap at version 2 per requirement, no further increment
            int next = section.getVersion() != null ? section.getVersion() + 1 : 1;
            section.setVersion(Math.min(next, 2));
        }
        section.setUpdatedAt(LocalDateTime.now());
        return projectMapper.toPaperSectionResponse(paperSectionRepository.save(section));
    }

    @Override
    @Transactional
    public PaperSectionResponse assignSection(UUID documentId, UUID sectionId, UUID assignedUserId) {
        requireDocumentWriteAccess(documentId);
        User currentUser = currentUserService.requireCurrentUser();
        PaperSection section = requireSectionInDocument(sectionId, documentId);
        if (assignedUserId != null) {
            User user = userRepository.findById(assignedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(assignedUserId, "User"));
            section.setAssignedUser(user);
        } else {
            section.setAssignedUser(null);
        }
        section.setUpdatedAt(LocalDateTime.now());
        PaperSectionResponse response = projectMapper.toPaperSectionResponse(paperSectionRepository.save(section));
        if (assignedUserId != null) {
            systemNotificationService.createNotification(
                    section.getAssignedUser(),
                    currentUser,
                    "SECTION_ASSIGNED",
                    sectionId,
                    currentUser.getEmail() + " assigned you to section \"" + section.getSectionTitle() + "\".");
        }
        return response;
    }

    @Override
    @Transactional
    public PaperSectionResponse rollbackSection(UUID documentId, UUID sectionId) {
        requireDocumentWriteAccess(documentId);
        User currentUser = currentUserService.requireCurrentUser();
        PaperSection section = requireSectionInDocument(sectionId, documentId);
        currentUserService.requireSectionAssignment(currentUser, section);
        if (section.getPreviousContentTex() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No previous version to rollback to.");
        }
        String current = section.getContentTex();
        section.setContentTex(section.getPreviousContentTex());
        section.setPreviousContentTex(current);
        section.setVersion(section.getVersion() != null ? section.getVersion() - 1 : 0);
        section.setUpdatedAt(LocalDateTime.now());
        return projectMapper.toPaperSectionResponse(paperSectionRepository.save(section));
    }

    @Override
    @Transactional
    public void deleteSection(UUID documentId, UUID sectionId) {
        requireDocumentWriteAccess(documentId);
        User currentUser = currentUserService.requireCurrentUser();
        PaperSection section = requireSectionInDocument(sectionId, documentId);
        currentUserService.requireSectionAssignment(currentUser, section);
        section.setActive(false);
        section.setUpdatedAt(LocalDateTime.now());
        paperSectionRepository.save(section);
    }

    @Override
    @Transactional
    public PaperSectionResponse createSection(UUID documentId, String title, UUID parentSectionId) {
        Document document = requireDocumentWriteAccess(documentId);

        List<PaperSection> existing = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(documentId);
        int maxOrder = existing.stream()
                .mapToInt(PaperSection::getSectionOrder)
                .max()
                .orElse(-1);

        PaperSection section = new PaperSection();
        section.setDocument(document);
        section.setSectionTitle(title != null ? title : "New Section");
        section.setSectionOrder(maxOrder + 1);
        section.setContentTex("");
        section.setUpdatedAt(LocalDateTime.now());
        if (parentSectionId != null) {
            PaperSection parent = requireSectionInDocument(parentSectionId, documentId);
            section.setSectionOrder(parent.getSectionOrder() + 1);
        }
        return projectMapper.toPaperSectionResponse(paperSectionRepository.save(section));
    }

    @Override
    @Transactional
    public List<PaperSectionResponse> createSectionsFromStandard(UUID documentId, String standard) {
        Document document = requireDocumentWriteAccess(documentId);
        PaperStandard paperStandard;
        try {
            paperStandard = PaperStandard.valueOf(standard);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown standard: " + standard);
        }

        List<String> requiredSections = paperStandardService.getRequiredSections(paperStandard);
        if (requiredSections.isEmpty()) {
            return List.of();
        }

        List<PaperSection> existing = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(documentId);
        int startOrder = existing.stream()
                .mapToInt(PaperSection::getSectionOrder)
                .max()
                .orElse(-1) + 1;

        List<PaperSection> sections = new ArrayList<>();
        for (int i = 0; i < requiredSections.size(); i++) {
            PaperSection section = new PaperSection();
            section.setDocument(document);
            section.setSectionTitle(requiredSections.get(i));
            section.setSectionOrder(startOrder + i);
            section.setContentTex("");
            section.setUpdatedAt(LocalDateTime.now());
            sections.add(section);
        }

        return paperSectionRepository.saveAll(sections).stream()
                .map(projectMapper::toPaperSectionResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<PaperSectionResponse> resetSectionsForStandard(UUID projectId, String standard) {
        // 1. Validate the standard value early — fail fast before any DB writes.
        PaperStandard paperStandard;
        try {
            paperStandard = PaperStandard.valueOf(standard);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown standard: " + standard);
        }

        // 2. Resolve the project and verify the caller has write access.
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireProjectWriteAccess(currentUser, project);

        // 3. Find the project's single active Paper (1 Project : 1 Paper invariant).
        List<Document> papers = documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(projectId, DocumentType.PAPER);

        // 4. No paper exists yet — create a stub and generate sections (same flow as /papers/init).
        if (papers.isEmpty()) {
            Document stub = new Document();
            stub.setProject(project);
            stub.setUploadedBy(currentUser);
            stub.setDocType(DocumentType.PAPER);
            stub.setFileUrl("placeholder");
            stub.setOriginalFilename("_standard_" + paperStandard.name() + ".tex");
            stub.setContentType("text/plain");
            stub.setFileSizeBytes(0L);
            stub.setProcessingStatus(ProcessingStatus.READY);
            stub.setActive(true);
            stub.setCreatedAt(java.time.LocalDateTime.now());
            stub.setDownloadToken(UUID.randomUUID().toString());
            stub = documentRepository.save(stub);
            return createSectionsFromStandard(stub.getId(), standard);
        }

        Document paper = papers.getFirst();
        // ponytail: update filename to reflect the new standard
        paper.setOriginalFilename("_standard_" + paperStandard.name() + ".tex");
        paper = documentRepository.save(paper);

        // 5. Load all current sections for the paper.
        List<PaperSection> existingSections = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(paper.getId());

        // 6. Guard: refuse if any section is currently assigned to a student.
        //    The frontend enforces this via hasAssignedSections lock, but the backend
        //    must be the authoritative gate to prevent data loss from direct API calls.
        boolean hasAssigned = existingSections.stream()
                .anyMatch(s -> s.getAssignedUser() != null);
        if (hasAssigned) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot reset standard: one or more sections are assigned to students. "
                    + "Unassign all sections before changing the standard.");
        }

        // ponytail: guard — refuse if any section contains student work content
        boolean hasContent = existingSections.stream()
                .anyMatch(s -> s.getContentTex() != null && !s.getContentTex().isBlank());
        if (hasContent) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot reset standard: one or more sections contain student work. "
                    + "Clear section content before changing the standard.");
        }

        // 7. Delete SectionFeedback rows first.
        //    FK: section_feedback.section_id is NOT NULL — must be cleared before
        //    PaperSection rows can be deleted, or the DB throws a constraint violation.
        List<UUID> sectionIds = existingSections.stream()
                .map(PaperSection::getId)
                .toList();
        if (!sectionIds.isEmpty()) {
            sectionFeedbackRepository.deleteAllBySectionIdIn(sectionIds);
        }

        // 8. Hard-delete all PaperSection rows for this paper.
        //    Soft-delete (active=false) cannot be used: createSectionsFromStandard
        //    computes startOrder from ALL rows (no active filter), so inactive rows
        //    would cause an off-by-N offset on the new sections.
        paperSectionRepository.deleteByDocumentId(paper.getId());

        // 9. Re-create sections from the new standard on a now-clean paper.
        //    createSectionsFromStandard now starts at sectionOrder = 0.
        return createSectionsFromStandard(paper.getId(), standard);
    }

    private PaperSection requireSectionInDocument(UUID sectionId, UUID documentId) {
        PaperSection section = paperSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
        if (!documentId.equals(section.getDocument().getId())) {
            throw new ResourceNotFoundException(sectionId, "PaperSection");
        }
        return section;
    }

    private List<PaperSection> parseSections(String text, Document document) {
        Pattern pattern = Pattern.compile("(?m)^(?:#{1,6}\\s+)?([A-Z][A-Za-z\\s]+)\\s*\\n");
        Matcher matcher = pattern.matcher(text);

        List<PaperSection> sections = new ArrayList<>();
        int index = 0;
        int lastEnd = 0;

        while (matcher.find()) {
            String sectionName = matcher.group(1).trim();
            int start = matcher.start();

            if (index > 0) {
                sections.get(index - 1).setContentTex(text.substring(lastEnd, start).trim());
            }

            PaperSection section = new PaperSection();
            section.setDocument(document);
            section.setSectionOrder(index);
            section.setSectionTitle(sectionName);
            sections.add(section);

            lastEnd = matcher.end();
            index++;
        }

        if (!sections.isEmpty()) {
            sections.get(sections.size() - 1).setContentTex(text.substring(lastEnd).trim());
        }

        if (sections.isEmpty()) {
            PaperSection section = new PaperSection();
            section.setDocument(document);
            section.setSectionOrder(0);
            section.setSectionTitle("Full Text");
            section.setContentTex(text);
            sections.add(section);
        }

        return sections;
    }

    private Document requireDocumentAccess(UUID documentId) {
        User currentUser = currentUserService.requireCurrentUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
        if (document.getProject() != null) {
            currentUserService.requireProjectAccess(currentUser, document.getProject());
            return document;
        }
        currentUserService.requireUserIdOrAdmin(currentUser, document.getUploadedBy().getId());
        return document;
    }

    private Document requireDocumentWriteAccess(UUID documentId) {
        Document document = requireDocumentAccess(documentId);
        if (document.getProject() != null) {
            currentUserService.requireProjectWriteAccess(
                    currentUserService.requireCurrentUser(), document.getProject());
        }
        return document;
    }

    @Override
    public Path exportTexArchive(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);

        List<Document> docs = documentRepository.findByProjectId(projectId);
        Path destination;
        try {
            destination = Files.createTempFile("evidencepilot-project-export-", ".zip");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create export archive", e);
        }
        try (OutputStream output = Files.newOutputStream(destination);
                ZipOutputStream zos = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Document doc : docs) {
                String filename = (doc.getOriginalFilename() != null
                        ? doc.getOriginalFilename().replaceAll("\\.[^.]+$", "")
                        : "paper-" + doc.getId()) + ".tex";
                List<PaperSection> sections = paperSectionRepository
                        .findByDocumentIdOrderBySectionOrderAsc(doc.getId());
                StringBuilder tex = new StringBuilder();
                String title = doc.getTitle() != null ? doc.getTitle() : (doc.getOriginalFilename() != null ? doc.getOriginalFilename() : "Untitled");
                tex.append("\\documentclass{article}\n")
                        .append("\\usepackage[utf8]{inputenc}\n")
                        .append("\\usepackage{graphicx}\n")
                        .append("\\usepackage{xcolor}\n")
                        .append("\\usepackage{soul}\n\n")
                        .append("\\title{").append(escapeLatex(title)).append("}\n")
                        .append("\\author{").append(escapeLatex(project.getTitle())).append("}\n")
                        .append("\\date{\\today}\n\n")
                        .append("\\begin{document}\n\n")
                        .append("\\maketitle\n\n");
                for (PaperSection section : sections) {
                    if (section.getContentTex() != null && !section.getContentTex().isBlank()) {
                        tex.append("\\section{").append(escapeLatex(section.getSectionTitle())).append("}\n\n");
                        tex.append(section.getContentTex()).append("\n\n");
                    }
                }
                tex.append("\\end{document}\n");
                ZipEntry entry = new ZipEntry(filename);
                entry.setTime(System.currentTimeMillis());
                zos.putNextEntry(entry);
                zos.write(tex.toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            texArchiveMediaWriter.writeProjectMedia(projectId, zos);
            return destination;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(destination);
            } catch (Exception cleanupException) {
                e.addSuppressed(cleanupException);
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Failed to create export archive", e);
        }
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
