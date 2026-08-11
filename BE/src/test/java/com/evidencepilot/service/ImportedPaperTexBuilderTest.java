package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImportedPaperTexBuilderTest {

    private final ImportedPaperTexBuilder builder = new ImportedPaperTexBuilder();

    @Test
    void buildsEditableTwoColumnPaperWithFrontMatterAndPageFurniture() {
        Document document = new Document();
        document.setOriginalFilename("reference.pdf");
        List<AiModelClient.ExtractionPage> pages = List.of(
                new AiModelClient.ExtractionPage(0, 1000.0, 1400.0),
                new AiModelClient.ExtractionPage(1, 1000.0, 1400.0),
                new AiModelClient.ExtractionPage(2, 1000.0, 1400.0));
        List<AiModelClient.ExtractionBlock> blocks = List.of(
                block("heading", "Evaluation of adipokines", 1, 0, 100, 100, 900, 180),
                block("paragraph", "A. Author & B. Author", null, 0, 100, 200, 900, 240),
                block("heading", "Abstract", 2, 0, 100, 300, 900, 330),
                block("paragraph", "This study evaluates biomarkers.", null, 0, 100, 350, 900, 430),
                block("heading", "Introduction", 2, 0, 100, 470, 470, 500),
                block("paragraph", "Left column.", null, 0, 100, 520, 470, 620),
                block("paragraph", "Right column.", null, 0, 530, 520, 900, 620),
                block("paragraph", "Page two left.", null, 1, 100, 120, 470, 220),
                block("paragraph", "Page two right.", null, 1, 530, 120, 900, 220),
                new AiModelClient.ExtractionBlock(
                        "table", "| A | B |\n| --- | --- |\n| 1 | 2 |", null, "Table I",
                        "table", 1, List.of(100.0, 300.0, 900.0, 600.0), null),
                new AiModelClient.ExtractionBlock(
                        "figure", "Figure 1", null, "Figure 1", "chart", 1,
                        List.of(100.0, 650.0, 900.0, 950.0), "images/chart.png"),
                block("heading", "Discussion", 2, 2, 100, 200, 470, 230),
                block("paragraph", "Discussion body.", null, 2, 100, 250, 470, 350),
                block("paragraph", "More discussion.", null, 2, 530, 250, 900, 350),
                furniture("header", "Evaluation of adipokines", 1, 100, 30, 400, 55),
                furniture("header", "A. Author et al.", 2, 600, 30, 900, 55),
                furniture("footer", "Journal 2022; 60/3", 1, 100, 1340, 350, 1370),
                furniture("page_number", "193", 1, 900, 30, 940, 55),
                furniture("page_number", "194", 2, 60, 30, 100, 55),
                new AiModelClient.ExtractionBlock(
                        "reference", "References", null, null, "text", 2,
                        List.of(100.0, 900.0, 470.0, 930.0), null),
                new AiModelClient.ExtractionBlock(
                        "reference", "Smith 2024", null, null, "text", 2,
                        List.of(100.0, 950.0, 470.0, 1000.0), null));

        ImportedPaperTexBuilder.ImportedPaper paper = builder.build(document, blocks, pages);

        assertThat(paper.preambleTex())
                .contains("twocolumn")
                .contains("\\setcounter{page}{192}")
                .contains("Evaluation of adipokines")
                .contains("Journal 2022; 60/3");
        assertThat(paper.frontMatterTex())
                .contains("Evaluation of adipokines")
                .contains("A. Author \\& B. Author")
                .contains("\\begin{abstract}")
                .contains("This study evaluates biomarkers.");
        assertThat(paper.sections()).extracting(ImportedPaperTexBuilder.SectionTex::title)
                .containsExactly("Introduction", "Discussion", "References");
        assertThat(paper.sections().get(0).contentTex())
                .contains("\\begin{table*}")
                .contains("\\begin{figure*}")
                .contains("images/chart.png");
    }

    private static AiModelClient.ExtractionBlock block(
            String type,
            String text,
            Integer level,
            int page,
            double x1,
            double y1,
            double x2,
            double y2) {
        return new AiModelClient.ExtractionBlock(
                type, text, level, null, "text", page, List.of(x1, y1, x2, y2), null);
    }

    private static AiModelClient.ExtractionBlock furniture(
            String type,
            String text,
            int page,
            double x1,
            double y1,
            double x2,
            double y2) {
        return new AiModelClient.ExtractionBlock(
                type, text, null, null, type, page, List.of(x1, y1, x2, y2), null);
    }
}
