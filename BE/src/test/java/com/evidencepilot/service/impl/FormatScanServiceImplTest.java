package com.evidencepilot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.evidencepilot.dto.response.CitationValidationResponse;
import com.evidencepilot.dto.response.FormatScanResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.CitationValidationService;
import com.evidencepilot.service.CurrentUserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class FormatScanServiceImplTest {

    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final PaperSectionRepository paperSectionRepository = mock(PaperSectionRepository.class);
    private final CitationValidationService citationValidationService = mock(CitationValidationService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final SourceMatchingService sourceMatchingService = mock(SourceMatchingService.class);

    @Test
    void flagsOnlyClearlyMarkedQuotationsAtFortyWords() {
        List<String> markedQuotations = List.of(
                "\\begin{quote}" + words(40) + "\\end{quote}",
                "\\begin{quotation}" + words(40) + "\\end{quotation}",
                "``" + words(40) + "''",
                "“" + words(40) + "”",
                "\"" + words(40) + "\"");

        for (String quotation : markedQuotations) {
            List<FormatScanResponse.ScanFinding> findings = excessiveQuotationFindings(scan(quotation));
            assertThat(findings).singleElement().satisfies(finding -> {
                assertThat(finding.severity()).isEqualTo("WARN");
                assertThat(finding.message()).contains("40 words");
            });
        }

        assertThat(excessiveQuotationFindings(scan("\\begin{quote}" + words(39) + "\\end{quote}"))).isEmpty();
        assertThat(excessiveQuotationFindings(scan(words(40)))).isEmpty();
        assertThat(excessiveQuotationFindings(scan("\\begin{quote}" + words(40)))).isEmpty();
        assertThat(excessiveQuotationFindings(scan("% \"" + words(40) + "\""))).isEmpty();
    }

    @Test
    void resolvesGeneratedCitationsForPreview() {
        UUID sourceId = UUID.randomUUID();
        Document source = new Document();
        source.setId(sourceId);
        source.setTitle("Evidence Source");
        source.setAuthors("A. Researcher");
        source.setPublicationYear(2026);
        String key = SourceMatchingService.citationKey(sourceId);

        FormatScanResponse response = scan(
                "Supported claim \\cite{" + key + "}. Repeated \\cite{" + key + "}.", source);

        assertThat(response.citationNumbers()).containsEntry(key, 1);
        assertThat(response.references()).singleElement().satisfies(reference -> {
            assertThat(reference.number()).isEqualTo(1);
            assertThat(reference.reference()).contains("A. Researcher", "Evidence Source", "2026");
        });
    }

    private FormatScanResponse scan(String content) {
        return scan(content, null);
    }

    private FormatScanResponse scan(String content, Document source) {
        Document document = new Document();
        document.setTitle("Paper");
        Project project = new Project();
        project.setId(UUID.randomUUID());
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setSectionTitle("Introduction");
        section.setContentTex(content);
        when(currentUserService.requireCurrentUser()).thenReturn(new User());
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(DOCUMENT_ID)).thenReturn(List.of(section));
        when(citationValidationService.validateCitations(DOCUMENT_ID)).thenReturn(new CitationValidationResponse(
                true, "Paper", 1, 1, List.of(), List.of(), List.of(), PaperStandard.IEEE, null));
        when(sourceMatchingService.activeSources(project.getId()))
                .thenReturn(source == null ? List.of() : List.of(source));
        return new FormatScanServiceImpl(
                documentRepository,
                paperSectionRepository,
                citationValidationService,
                currentUserService,
                sourceMatchingService)
                .scanFormat(DOCUMENT_ID);
    }

    private static List<FormatScanResponse.ScanFinding> excessiveQuotationFindings(FormatScanResponse response) {
        return response.findings().stream()
                .filter(finding -> "EXCESSIVE_QUOTATION".equals(finding.category()))
                .toList();
    }

    private static String words(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "word")
                .collect(Collectors.joining(" "));
    }
}
