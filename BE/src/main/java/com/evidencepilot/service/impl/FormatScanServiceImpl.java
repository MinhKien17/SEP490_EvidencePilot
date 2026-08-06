package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.CitationValidationResponse;
import com.evidencepilot.dto.response.FormatScanResponse;
import com.evidencepilot.dto.response.FormatScanResponse.ScanFinding;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.CitationValidationService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.FormatScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FormatScanServiceImpl implements FormatScanService {

    private static final Pattern FIRST_PERSON = Pattern.compile(
            "\\b(I|we|my|our|us|mine|ours)\\b", Pattern.CASE_INSENSITIVE);
    private static final int ABSTRACT_MAX_WORDS = 250;
    private static final Pattern CITE_PATTERN = Pattern.compile("\\\\cite(?:\\[[^\\]]*\\])?\\{[^}]+\\}");

    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final CitationValidationService citationValidationService;
    private final CurrentUserService currentUserService;

    @Override
    public FormatScanResponse scanFormat(UUID documentId) {
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));
        currentUserService.requireProjectAccess(currentUser, doc.getProject());

        List<ScanFinding> findings = new ArrayList<>();
        List<PaperSection> sections = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(documentId);

        CitationValidationResponse citationResult = citationValidationService.validateCitations(documentId);

        for (PaperSection section : sections) {
            String tex = section.getContentTex();
            if (tex == null || tex.isBlank()) continue;
            String title = section.getSectionTitle() != null ? section.getSectionTitle().toLowerCase() : "";

            checkFirstPerson(tex, title, findings);
            checkAbstractRules(tex, title, findings);
        }

        checkCitationCoverage(citationResult, findings);

        String paperTitle = doc.getTitle() != null ? doc.getTitle() : doc.getOriginalFilename();
        return new FormatScanResponse(paperTitle, findings);
    }

    private void checkFirstPerson(String tex, String sectionTitle, List<ScanFinding> findings) {
        var matcher = FIRST_PERSON.matcher(tex);
        if (matcher.find()) {
            findings.add(new ScanFinding("TONE", "WARN",
                    sectionTitle.isEmpty() ? "general" : sectionTitle,
                    "First-person pronouns detected (I, we, our, etc.). Academic writing prefers passive voice.",
                    "Replace 'I argue' with 'It is argued that...' or 'The analysis indicates...'"));
        }
    }

    private void checkAbstractRules(String tex, String sectionTitle, List<ScanFinding> findings) {
        if (!sectionTitle.contains("abstract")) return;

        String plain = tex.replaceAll("\\\\[a-zA-Z]+(\\{[^}]*\\})?", "").strip();
        int wordCount = plain.isEmpty() ? 0 : plain.split("\\s+").length;
        if (wordCount > ABSTRACT_MAX_WORDS) {
            findings.add(new ScanFinding("STRUCTURE", "WARN",
                    sectionTitle,
                    "Abstract exceeds " + ABSTRACT_MAX_WORDS + " words (found: " + wordCount + ").",
                    "Shorten abstract to " + ABSTRACT_MAX_WORDS + " words or fewer."));
        }

        if (CITE_PATTERN.matcher(tex).find()) {
            findings.add(new ScanFinding("STRUCTURE", "ERROR",
                    sectionTitle,
                    "Abstract contains citations. Abstracts should not include \\cite{}.",
                    "Remove citations from the abstract."));
        }
    }

    private void checkCitationCoverage(CitationValidationResponse citationResult, List<ScanFinding> findings) {
        if (citationResult.totalCitations() == 0) {
            findings.add(new ScanFinding("CITATIONS", "WARN",
                    "general",
                    "No citations found. Use \\cite{key} to reference sources.",
                    "Add \\cite{key} where you reference external sources."));
        }
        for (String key : citationResult.missingCitations()) {
            findings.add(new ScanFinding("CITATIONS", "ERROR",
                    "general",
                    "Citation key '" + key + "' not found in project sources.",
                    "Add the source document or fix the citation key."));
        }
        for (String key : citationResult.unmatchedKeys()) {
            findings.add(new ScanFinding("CITATIONS", "WARN",
                    "general",
                    "Citation key '" + key + "' has no \\bibitem definition.",
                    "Add \\bibitem{" + key + "} or check for typos."));
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
