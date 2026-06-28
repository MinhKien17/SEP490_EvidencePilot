package com.evidencepilot.service.impl;

import com.evidencepilot.infrastructure.AiModelClient;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.PaperProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperProcessingServiceImpl implements PaperProcessingService {

    private static final int REVIEW_TEXT_LIMIT = 10_000;

    private final AiModelClient aiModelClient;
    private final PaperSectionRepository paperSectionRepository;

    @Override
    @Transactional
    public List<PaperSection> detectAndPersistSections(Document document) {
        String text = document.getDocumentText() != null
                ? document.getDocumentText().getExtractedText() : null;
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<PaperSection> sections = parseSections(text, document);
        return paperSectionRepository.saveAll(sections);
    }

    @Override
    public Map<String, Object> review(Document document, String targetStyle) {
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
            throw new org.springframework.web.server.ResponseStatusException(
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
