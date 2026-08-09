package com.evidencepilot.service;

import com.evidencepilot.dto.response.PaperStandardSuggestionResponse;
import com.evidencepilot.model.enums.PaperStandard;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PaperStandardService {

    private static final int DETECTION_SAMPLE_CHARS = 20_000;
    private static final Pattern COMMENT = Pattern.compile("(?m)(?<!\\\\)%.*$");
    private static final Pattern WORKS_CITED_HEADING = Pattern.compile(
            "(?im)^(?:#{1,6}\\h+)?works cited\\h*$");

    private static final Map<PaperStandard, List<String>> STANDARD_SECTIONS = Map.of(
        PaperStandard.IEEE, List.of("Abstract", "Introduction", "Methodology", "Results", "Discussion", "Conclusion", "References"),
        PaperStandard.ACM, List.of("Abstract", "Introduction", "Methodology", "Results", "Discussion", "Conclusion", "References"),
        PaperStandard.SPRINGER_LNCS, List.of("Abstract", "Introduction", "Methodology", "Results", "Discussion", "Conclusion", "References"),
        PaperStandard.APA, List.of("Abstract", "Introduction", "Method", "Results", "Discussion", "References"),
        PaperStandard.MLA, List.of("Abstract", "Introduction", "Body", "Conclusion", "Works Cited"),
        PaperStandard.CUSTOM, List.of()
    );

    private static final Map<String, String> TITLE_VARIANTS = Map.ofEntries(
        Map.entry("intro", "Introduction"),
        Map.entry("introduction", "Introduction"),
        Map.entry("background", "Introduction"),
        Map.entry("related work", "Introduction"),
        Map.entry("related works", "Introduction"),
        Map.entry("literature review", "Introduction"),
        Map.entry("method", "Methodology"),
        Map.entry("methods", "Methodology"),
        Map.entry("methodology", "Methodology"),
        Map.entry("approach", "Methodology"),
        Map.entry("experimental setup", "Methodology"),
        Map.entry("experiment", "Methodology"),
        Map.entry("experiments", "Methodology"),
        Map.entry("result", "Results"),
        Map.entry("results", "Results"),
        Map.entry("finding", "Results"),
        Map.entry("findings", "Results"),
        Map.entry("discussion", "Discussion"),
        Map.entry("conclusion", "Conclusion"),
        Map.entry("conclusions", "Conclusion"),
        Map.entry("summary", "Conclusion"),
        Map.entry("future work", "Conclusion"),
        Map.entry("reference", "References"),
        Map.entry("references", "References"),
        Map.entry("bibliography", "References"),
        Map.entry("works cited", "Works Cited"),
        Map.entry("abstract", "Abstract"),
        Map.entry("body", "Body")
    );

    public List<String> getRequiredSections(PaperStandard standard) {
        return STANDARD_SECTIONS.getOrDefault(standard, List.of());
    }

    public String getSectionTemplate(PaperStandard standard, String title) {
        return "% EvidencePilot " + standard.name() + " template\n"
                + "% " + guidance(title) + "\n";
    }

    public boolean hasStudentContent(String content) {
        return content != null && !COMMENT.matcher(content).replaceAll("").isBlank();
    }

    public PaperStandardSuggestionResponse suggestStandard(String filename, String extractedText) {
        String sample = detectionSample(extractedText);
        String firstPages = firstPages(extractedText);

        // ponytail: deterministic format markers only; add a classifier only if audited
        // uploads show these high-confidence rules miss real templates.
        if (firstPages.contains("ieeetran")) {
            return suggestion(PaperStandard.IEEE, 99, "IEEEtran");
        }
        if (firstPages.contains("acmart")) {
            return suggestion(PaperStandard.ACM, 99, "acmart");
        }
        if (firstPages.contains("llncs") || firstPages.contains("splncs04")) {
            return suggestion(PaperStandard.SPRINGER_LNCS, 99, "llncs/splncs04");
        }
        if (firstPages.contains("{apa7}") || firstPages.contains("{apa6}")) {
            return suggestion(PaperStandard.APA, 99, "apa6/apa7");
        }
        if (firstPages.contains("\\documentclass{mla}")) {
            return suggestion(PaperStandard.MLA, 99, "documentclass{mla}");
        }
        if (firstPages.contains("acm reference format")) {
            return suggestion(PaperStandard.ACM, 95, "ACM Reference Format");
        }
        if (firstPages.contains("lecture notes in computer science")) {
            return suggestion(PaperStandard.SPRINGER_LNCS, 95, "Lecture Notes in Computer Science");
        }
        if (firstPages.contains("republication/redistribution requires ieee permission")) {
            return suggestion(PaperStandard.IEEE, 95, "IEEE publication notice");
        }

        String normalizedFilename = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (filenameHasToken(normalizedFilename, "ieee")) {
            return suggestion(PaperStandard.IEEE, 85, "filename: IEEE");
        }
        if (filenameHasToken(normalizedFilename, "acm")) {
            return suggestion(PaperStandard.ACM, 85, "filename: ACM");
        }
        if (filenameHasToken(normalizedFilename, "lncs")) {
            return suggestion(PaperStandard.SPRINGER_LNCS, 85, "filename: LNCS");
        }
        if (filenameHasToken(normalizedFilename, "apa")) {
            return suggestion(PaperStandard.APA, 85, "filename: APA");
        }
        if (filenameHasToken(normalizedFilename, "mla")) {
            return suggestion(PaperStandard.MLA, 85, "filename: MLA");
        }
        if (WORKS_CITED_HEADING.matcher(sample).find()) {
            return suggestion(PaperStandard.MLA, 75, "Works Cited");
        }
        return new PaperStandardSuggestionResponse(PaperStandard.CUSTOM, 0, List.of());
    }

    public String renderTemplate(PaperStandard standard, String title, String body) {
        PaperStandard resolved = standard == null ? PaperStandard.CUSTOM : standard;
        String resource = "paper-templates/"
                + resolved.name().toLowerCase(java.util.Locale.ROOT) + ".tex";
        try {
            return new ClassPathResource(resource)
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{TITLE}}", title)
                    .replace("{{BODY}}", body);
        } catch (IOException exception) {
            throw new IllegalStateException("Missing TeX template: " + resource, exception);
        }
    }

    public String normalizeSectionTitle(String title) {
        if (title == null) return "";
        String lower = title.trim().toLowerCase();
        String normalized = TITLE_VARIANTS.get(lower);
        return normalized != null ? normalized : title.trim();
    }

    private static String guidance(String title) {
        return switch (title) {
            case "Abstract" -> "Summarize the problem, method, main result, and contribution.";
            case "Introduction" -> "Explain the context, research gap, objectives, and contributions.";
            case "Method", "Methodology" -> "Describe the design, data, procedure, and evaluation method.";
            case "Results" -> "Present findings and cite external baselines, methods, or comparisons.";
            case "Discussion" -> "Interpret results, limitations, threats, and implications.";
            case "Conclusion" -> "Summarize contributions and justified future work.";
            case "References", "Works Cited" -> "Add references using the selected paper standard.";
            default -> "Write this section and cite external facts, methods, data, and prior work.";
        };
    }

    private static PaperStandardSuggestionResponse suggestion(
            PaperStandard standard,
            int confidencePercent,
            String evidence) {
        return new PaperStandardSuggestionResponse(standard, confidencePercent, List.of(evidence));
    }

    private static String detectionSample(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= DETECTION_SAMPLE_CHARS * 2) {
            return text.toLowerCase(Locale.ROOT);
        }
        return (text.substring(0, DETECTION_SAMPLE_CHARS)
                + '\n'
                + text.substring(text.length() - DETECTION_SAMPLE_CHARS))
                .toLowerCase(Locale.ROOT);
    }

    private static String firstPages(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.substring(0, Math.min(text.length(), DETECTION_SAMPLE_CHARS))
                .toLowerCase(Locale.ROOT);
    }

    private static boolean filenameHasToken(String filename, String token) {
        String words = " " + filename.replaceAll("[^a-z0-9]+", " ") + " ";
        return words.contains(" " + token + " ");
    }
}
