package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.PaperValidationResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.PaperProcessingService;
import com.evidencepilot.service.PaperStandardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperProcessingServiceImpl implements PaperProcessingService {

    private static final int REVIEW_TEXT_LIMIT = 10_000;

    private final AiModelClient aiModelClient;
    private final PaperSectionRepository paperSectionRepository;
    private final DocumentRepository documentRepository;
    private final ProjectMapper projectMapper;
    private final CurrentUserService currentUserService;
    private final PaperStandardService paperStandardService;

    @Override
    public List<PaperSectionResponse> getPaperSections(UUID documentId) {
        requireDocumentAccess(documentId);
        return paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(documentId).stream()
                .map(projectMapper::toPaperSectionResponse)
                .toList();
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
            String title, Integer order, UUID mergeIntoId) {
        requireDocumentWriteAccess(documentId);

        if (mergeIntoId != null) {
            PaperSection target = requireSectionInDocument(mergeIntoId, documentId);
            PaperSection source = requireSectionInDocument(sectionId, documentId);
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
        if (title != null && !title.isBlank()) {
            section.setSectionTitle(title);
        }
        if (order != null) {
            section.setSectionOrder(order);
        }
        section.setUpdatedAt(LocalDateTime.now());
        return projectMapper.toPaperSectionResponse(paperSectionRepository.save(section));
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
}
