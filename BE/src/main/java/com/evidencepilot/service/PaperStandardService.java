package com.evidencepilot.service;

import com.evidencepilot.model.enums.PaperStandard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PaperStandardService {

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

    public String normalizeSectionTitle(String title) {
        if (title == null) return "";
        String lower = title.trim().toLowerCase();
        String normalized = TITLE_VARIANTS.get(lower);
        return normalized != null ? normalized : title.trim();
    }
}