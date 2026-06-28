package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.PaperProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperProcessingServiceImpl implements PaperProcessingService {

    private static final int REVIEW_TEXT_LIMIT = 10_000;

    private final AiModelClient aiModelClient;
    private final PaperSectionRepository paperSectionRepository;
    private final DocumentRepository documentRepository;
    private final ProjectMapper projectMapper;

    @Override
    public List<PaperSectionResponse> getPaperSections(UUID documentId) {
        return paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(documentId).stream()
                .map(projectMapper::toPaperSectionResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<PaperSectionResponse> detectAndPersistSections(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
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
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
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
}
