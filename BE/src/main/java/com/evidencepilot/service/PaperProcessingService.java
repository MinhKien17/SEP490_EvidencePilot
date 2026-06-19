package com.evidencepilot.service;

import com.evidencepilot.ai.dto.PaperReviewResponse;
import com.evidencepilot.ai.dto.SectionIssue;
import com.evidencepilot.domain.entity.Paper;
import com.evidencepilot.domain.entity.PaperSection;
import com.evidencepilot.repository.PaperSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaperProcessingService {

    private static final Map<String, List<String>> STYLE_RULES = Map.of(
            "conference", List.of("Abstract", "Introduction", "Related Work", "Methodology",
                    "Results", "Discussion", "Conclusion", "References"),
            "article", List.of("Title", "Introduction", "Main Argument", "Evidence", "Conclusion"),
            "magazine", List.of("Lead", "Main Story", "Examples", "Takeaway"),
            "report", List.of("Executive Summary", "Background", "Findings", "Recommendations", "Conclusion"),
            "thesis", List.of("Abstract", "Introduction", "Literature Review", "Methodology",
                    "Results", "Discussion", "Conclusion", "References")
    );

    private static final Set<String> KNOWN_HEADINGS = Set.of(
            "abstract", "introduction", "related work", "methodology", "methods", "results",
            "discussion", "conclusion", "references", "lead", "main story", "takeaway",
            "title", "main argument", "evidence", "executive summary", "background",
            "findings", "recommendations", "literature review"
    );

    private final PaperSectionRepository paperSectionRepository;

    public List<PaperSection> detectAndPersistSections(Paper paper) {
        List<DetectedSection> detected = detectSections(paper.getExtractedText());
        List<PaperSection> sections = new ArrayList<>();
        for (int index = 0; index < detected.size(); index++) {
            DetectedSection detectedSection = detected.get(index);
            PaperSection section = new PaperSection();
            section.setPaper(paper);
            section.setSectionIndex(index + 1);
            section.setName(detectedSection.name());
            section.setText(detectedSection.text());
            sections.add(section);
        }
        return paperSectionRepository.saveAll(sections);
    }

    public PaperReviewResponse review(Paper paper, String targetStyle) {
        List<PaperSection> sections = paperSectionRepository.findByPaperIdOrderBySectionIndex(paper.getId());
        String detectedStyle = detectStyle(sections);
        String chosenStyle = targetStyle == null || targetStyle.isBlank() ? detectedStyle : targetStyle;
        if ("unknown".equals(chosenStyle)) {
            chosenStyle = "article";
        }

        Set<String> presentNames = sections.stream()
                .map(section -> normalize(section.getName()))
                .collect(java.util.stream.Collectors.toSet());

        List<SectionIssue> missing = new ArrayList<>();
        for (String expected : STYLE_RULES.getOrDefault(chosenStyle, STYLE_RULES.get("article"))) {
            if (!presentNames.contains(normalize(expected)) && !hasAlias(expected, presentNames)) {
                missing.add(new SectionIssue(
                        expected,
                        expected + " is expected for a " + chosenStyle + " paper but was not found.",
                        "Add a " + expected + " section or rename the matching content clearly."
                ));
            }
        }

        List<SectionIssue> weak = sections.stream()
                .filter(section -> section.getText().split("\\s+").length < 18)
                .map(section -> new SectionIssue(
                        section.getName(),
                        "This section is short for its role in the paper.",
                        "Add clearer claims, supporting explanation, or evidence references."
                ))
                .toList();

        return new PaperReviewResponse(
                String.valueOf(paper.getId()),
                detectedStyle,
                chosenStyle,
                missing,
                weak,
                claimRecommendations(sections, chosenStyle)
        );
    }

    private List<DetectedSection> detectSections(String text) {
        String currentName = "Body";
        List<String> currentLines = new ArrayList<>();
        List<DetectedSection> sections = new ArrayList<>();

        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (looksLikeHeading(line)) {
                if (!currentLines.isEmpty()) {
                    sections.add(new DetectedSection(currentName, String.join("\n", currentLines).trim()));
                }
                currentName = line.replaceFirst(":$", "");
                currentLines = new ArrayList<>();
            } else {
                currentLines.add(line);
            }
        }
        if (!currentLines.isEmpty()) {
            sections.add(new DetectedSection(currentName, String.join("\n", currentLines).trim()));
        }
        return sections.isEmpty() ? List.of(new DetectedSection("Body", text.trim())) : sections;
    }

    private boolean looksLikeHeading(String line) {
        int words = line.split("\\s+").length;
        if (words > 8 || line.length() > 80) {
            return false;
        }
        String normalized = normalize(line.replaceFirst(":$", ""));
        return KNOWN_HEADINGS.contains(normalized)
                || line.equals(line.toUpperCase(Locale.ROOT))
                || Character.isUpperCase(line.charAt(0)) && !line.endsWith(".");
    }

    private String detectStyle(List<PaperSection> sections) {
        Set<String> present = sections.stream()
                .map(section -> normalize(section.getName()))
                .collect(java.util.stream.Collectors.toSet());
        String bestStyle = "unknown";
        int bestScore = 0;
        for (Map.Entry<String, List<String>> entry : STYLE_RULES.entrySet()) {
            int score = (int) entry.getValue().stream()
                    .map(this::normalize)
                    .filter(present::contains)
                    .count();
            if (score > bestScore) {
                bestStyle = entry.getKey();
                bestScore = score;
            }
        }
        return bestScore == 0 ? "unknown" : bestStyle;
    }

    private List<SectionIssue> claimRecommendations(List<PaperSection> sections, String style) {
        Set<String> claimWords = Set.of("claim", "argue", "shows", "demonstrates", "evidence", "support", "because");
        Set<String> checkedSections = Set.of("introduction", "related work", "discussion", "main argument", "findings");
        List<SectionIssue> recommendations = new ArrayList<>();

        for (PaperSection section : sections) {
            if (!checkedSections.contains(normalize(section.getName()))) {
                continue;
            }
            String text = section.getText().toLowerCase(Locale.ROOT);
            boolean hasSignal = claimWords.stream().anyMatch(text::contains);
            if (!hasSignal) {
                recommendations.add(new SectionIssue(
                        section.getName(),
                        "This section does not state enough explicit claims or evidence signals.",
                        "Add 1-2 clear claims that fit the " + style + " style and connect them to source evidence."
                ));
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add(new SectionIssue(
                    "Overall",
                    "Claim coverage should be checked against the paper's main argument.",
                    "Review each major section and ensure important assertions are tied to evidence."
            ));
        }
        return recommendations;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replace("_", " ");
    }

    private boolean hasAlias(String expected, Set<String> presentNames) {
        return switch (normalize(expected)) {
            case "methodology" -> presentNames.contains("methods");
            case "main story" -> presentNames.contains("body");
            case "takeaway" -> presentNames.contains("conclusion");
            default -> false;
        };
    }

    private record DetectedSection(String name, String text) {
    }
}
