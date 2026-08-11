package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImportedPaperTexBuilder {

    private static final Set<String> TOP_LEVEL = Set.of(
            "introduction", "background", "literature review", "related work",
            "materials and methods", "material and methods", "methods and materials",
            "methodology", "methods", "results", "results and discussion", "discussion",
            "conclusions", "conclusion", "conclusions and future work",
            "conclusion and future work", "acknowledgements", "acknowledgments",
            "references", "bibliography", "works cited", "appendix", "appendices");
    private static final Pattern HEADING_NUMBER = Pattern.compile(
            "^(?:\\d+(?:\\.\\d+)*|[IVXLCDM]+)[.)]?\\h+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_NUMBER = Pattern.compile("\\d+");
    private static final Pattern HTML_SUPERSCRIPT = Pattern.compile(
            "<sup>([^<>]*)</sup>", Pattern.CASE_INSENSITIVE);

    public boolean supports(List<AiModelClient.ExtractionBlock> blocks) {
        return blocks != null && blocks.stream()
                .anyMatch(block -> block != null
                        && block.pageIndex() != null
                        && block.bbox() != null);
    }

    public ImportedPaper build(
            Document document,
            List<AiModelClient.ExtractionBlock> blocks,
            List<AiModelClient.ExtractionPage> pages) {
        List<AiModelClient.ExtractionBlock> safeBlocks = blocks == null ? List.of() : blocks;
        Layout layout = detectLayout(safeBlocks, pages == null ? List.of() : pages);
        int firstSection = firstSectionIndex(safeBlocks);
        List<AiModelClient.ExtractionBlock> frontBlocks = firstSection < 0
                ? List.of()
                : safeBlocks.subList(0, firstSection);

        FrontMatter frontMatter = frontMatter(document, frontBlocks, layout.twoColumn());
        List<SectionTex> sections = sections(safeBlocks, firstSection, layout);
        if (sections.isEmpty()) {
            String content = renderBlocks(safeBlocks, layout);
            sections = List.of(new SectionTex("Full Text", content));
        }

        return new ImportedPaper(
                preamble(layout, safeBlocks),
                frontMatter.tex(),
                frontMatter.title(),
                frontMatter.authors(),
                sections);
    }

    private static int firstSectionIndex(List<AiModelClient.ExtractionBlock> blocks) {
        for (int index = 0; index < blocks.size(); index++) {
            if (topLevelTitle(blocks.get(index)) != null) {
                return index;
            }
        }
        return -1;
    }

    private static List<SectionTex> sections(
            List<AiModelClient.ExtractionBlock> blocks,
            int firstSection,
            Layout layout) {
        if (firstSection < 0) {
            return List.of();
        }
        List<SectionTex> result = new ArrayList<>();
        String title = null;
        StringBuilder content = new StringBuilder();
        for (int index = firstSection; index < blocks.size(); index++) {
            AiModelClient.ExtractionBlock block = blocks.get(index);
            String nextTitle = topLevelTitle(block);
            if (nextTitle != null) {
                if (title != null) {
                    result.add(new SectionTex(title, content.toString().strip()));
                }
                title = nextTitle;
                content.setLength(0);
                continue;
            }
            String rendered = renderBlock(block, layout);
            if (!rendered.isBlank()) {
                if (!content.isEmpty()) {
                    content.append("\n\n");
                }
                content.append(rendered);
            }
        }
        if (title != null) {
            result.add(new SectionTex(title, content.toString().strip()));
        }
        return result;
    }

    private static String renderBlocks(
            List<AiModelClient.ExtractionBlock> blocks,
            Layout layout) {
        StringBuilder content = new StringBuilder();
        for (AiModelClient.ExtractionBlock block : blocks) {
            String rendered = renderBlock(block, layout);
            if (!rendered.isBlank()) {
                if (!content.isEmpty()) {
                    content.append("\n\n");
                }
                content.append(rendered);
            }
        }
        return content.toString().strip();
    }

    private static String renderBlock(AiModelClient.ExtractionBlock block, Layout layout) {
        if (block == null || block.text() == null) {
            return "";
        }
        return switch (block.type()) {
            case "paragraph", "reference", "figure_caption" -> escapeText(block.text());
            case "heading" -> "\\subsection{" + escapeText(stripHeadingNumber(block.text())) + "}";
            case "list" -> renderList(block.text());
            case "equation" -> "\\[\n" + stripMathDelimiters(block.text()) + "\n\\]";
            case "code" -> "\\begin{verbatim}\n"
                    + block.text().replace("\\end{verbatim}", "end verbatim")
                    + "\n\\end{verbatim}";
            case "table" -> renderTable(block, layout);
            case "figure" -> renderFigure(block, layout);
            case "page_footnote" -> "\\footnotetext{" + escapeText(block.text()) + "}";
            default -> "";
        };
    }

    private static String renderList(String text) {
        List<String> items = text.lines()
                .map(line -> line.replaceFirst("^\\s*(?:[-+*]|\\d+[.)])\\s+", "").strip())
                .filter(line -> !line.isBlank())
                .toList();
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder tex = new StringBuilder("\\begin{itemize}\n");
        items.forEach(item -> tex.append("\\item ").append(escapeText(item)).append('\n'));
        return tex.append("\\end{itemize}").toString();
    }

    private static String renderTable(AiModelClient.ExtractionBlock block, Layout layout) {
        List<String[]> rows = block.text().lines()
                .map(ImportedPaperTexBuilder::markdownRow)
                .filter(row -> row.length > 0)
                .toList();
        if (rows.size() >= 2 && separatorRow(rows.get(1))) {
            rows = new ArrayList<>(rows);
            rows.remove(1);
        }
        if (rows.isEmpty()) {
            return escapeText(block.text());
        }
        int columns = rows.stream().mapToInt(row -> row.length).max().orElse(1);
        boolean wide = layout.wide(block);
        String environment = wide ? "table*" : "table";
        String width = wide ? "\\textwidth" : "\\columnwidth";
        StringBuilder tex = new StringBuilder("\\begin{").append(environment).append("}[tb]\n")
                .append("\\centering\n");
        if (block.caption() != null) {
            tex.append("\\caption{").append(escapeText(block.caption())).append("}\n");
        }
        tex.append("\\begin{tabularx}{").append(width).append("}{")
                .append("X".repeat(columns)).append("}\n\\toprule\n");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            String[] row = rows.get(rowIndex);
            for (int column = 0; column < columns; column++) {
                if (column > 0) {
                    tex.append(" & ");
                }
                tex.append(column < row.length ? escapeText(row[column]) : "");
            }
            tex.append(" \\\\\n");
            if (rowIndex == 0) {
                tex.append("\\midrule\n");
            }
        }
        return tex.append("\\bottomrule\n\\end{tabularx}\n\\end{")
                .append(environment).append('}').toString();
    }

    private static String[] markdownRow(String line) {
        String stripped = line.strip();
        if (!stripped.contains("|")) {
            return new String[0];
        }
        stripped = stripped.replaceFirst("^\\|", "").replaceFirst("\\|$", "");
        return Pattern.compile("\\|").splitAsStream(stripped)
                .map(String::strip)
                .toArray(String[]::new);
    }

    private static boolean separatorRow(String[] row) {
        for (String cell : row) {
            if (!cell.matches(":?-{3,}:?")) {
                return false;
            }
        }
        return true;
    }

    private static String renderFigure(AiModelClient.ExtractionBlock block, Layout layout) {
        if (block.assetPath() == null || !ExtractionBundle.validImagePath(block.assetPath())) {
            return block.caption() == null ? "" : escapeText(block.caption());
        }
        boolean wide = layout.wide(block);
        String environment = wide ? "figure*" : "figure";
        String width = wide ? "\\textwidth" : "\\columnwidth";
        StringBuilder tex = new StringBuilder("\\begin{").append(environment).append("}[tb]\n")
                .append("\\centering\n")
                .append("\\includegraphics[width=").append(width).append("]{")
                .append(block.assetPath()).append("}\n");
        if (block.caption() != null) {
            tex.append("\\caption{").append(escapeText(block.caption())).append("}\n");
        }
        return tex.append("\\end{").append(environment).append('}').toString();
    }

    private static FrontMatter frontMatter(
            Document document,
            List<AiModelClient.ExtractionBlock> blocks,
            boolean twoColumn) {
        int titleIndex = -1;
        int abstractIndex = -1;
        for (int index = 0; index < blocks.size(); index++) {
            AiModelClient.ExtractionBlock block = blocks.get(index);
            if (titleIndex < 0 && "heading".equals(block.type())
                    && !"abstract".equals(normalize(block.text()))) {
                titleIndex = index;
            }
            if ("heading".equals(block.type()) && "abstract".equals(normalize(block.text()))) {
                abstractIndex = index;
                break;
            }
        }

        String title = titleIndex >= 0 ? blocks.get(titleIndex).text() : firstNonBlank(
                document.getTitle(), document.getOriginalFilename(), "Untitled paper");
        List<String> authorLines = new ArrayList<>();
        int authorStart = titleIndex >= 0 ? titleIndex + 1 : 0;
        int authorEnd = abstractIndex >= 0 ? abstractIndex : blocks.size();
        for (int index = authorStart; index < authorEnd; index++) {
            AiModelClient.ExtractionBlock block = blocks.get(index);
            if ("paragraph".equals(block.type()) && block.text() != null && !block.text().isBlank()) {
                authorLines.add(block.text());
            }
        }
        String authors = authorLines.isEmpty() ? firstNonBlank(document.getAuthors(), "") : authorLines.get(0);

        StringBuilder abstractText = new StringBuilder();
        if (abstractIndex >= 0) {
            for (int index = abstractIndex + 1; index < blocks.size(); index++) {
                AiModelClient.ExtractionBlock block = blocks.get(index);
                if ("paragraph".equals(block.type()) || "list".equals(block.type())) {
                    if (!abstractText.isEmpty()) {
                        abstractText.append("\n\n");
                    }
                    abstractText.append(escapeText(block.text()));
                }
            }
        }

        String authorTex = authorLines.stream()
                .map(ImportedPaperTexBuilder::escapeText)
                .reduce((left, right) -> left + " \\\\[0.35em]\n" + right)
                .orElseGet(() -> escapeText(authors));
        StringBuilder content = new StringBuilder();
        if (twoColumn) {
            content.append("\\twocolumn[\n\\begin{minipage}{\\textwidth}\n\\centering\n")
                    .append("{\\LARGE\\bfseries ").append(escapeText(title)).append("\\par}\n")
                    .append("\\vspace{0.8em}\n").append(authorTex).append("\n\\vspace{0.8em}\n");
            if (!abstractText.isEmpty()) {
                content.append("\\begin{abstract}\n\\noindent ")
                        .append(abstractText).append("\n\\end{abstract}\n");
            }
            content.append("\\end{minipage}\n\\vspace{1em}\n]\n\\thispagestyle{firstpage}");
        } else {
            content.append("\\title{").append(escapeText(title)).append("}\n")
                    .append("\\author{").append(authorTex).append("}\n\\date{}\n\\maketitle\n")
                    .append("\\thispagestyle{firstpage}\n");
            if (!abstractText.isEmpty()) {
                content.append("\\begin{abstract}\n").append(abstractText)
                        .append("\n\\end{abstract}");
            }
        }
        return new FrontMatter(content.toString(), title, authors);
    }

    private static Layout detectLayout(
            List<AiModelClient.ExtractionBlock> blocks,
            List<AiModelClient.ExtractionPage> pages) {
        Map<Integer, AiModelClient.ExtractionPage> byPage = new LinkedHashMap<>();
        pages.stream().sorted(Comparator.comparingInt(AiModelClient.ExtractionPage::pageIndex))
                .forEach(page -> byPage.put(page.pageIndex(), page));
        for (AiModelClient.ExtractionBlock block : blocks) {
            if (block.pageIndex() == null || block.bbox() == null
                    || byPage.containsKey(block.pageIndex())) {
                continue;
            }
            byPage.put(block.pageIndex(), new AiModelClient.ExtractionPage(
                    block.pageIndex(), block.bbox().get(2), block.bbox().get(3)));
        }

        Set<Integer> bodyPages = new LinkedHashSet<>();
        Set<Integer> twoColumnPages = new LinkedHashSet<>();
        Map<Integer, boolean[]> bands = new LinkedHashMap<>();
        for (AiModelClient.ExtractionBlock block : blocks) {
            if (!bodyBlock(block) || block.pageIndex() == null || block.bbox() == null) {
                continue;
            }
            AiModelClient.ExtractionPage page = byPage.get(block.pageIndex());
            if (page == null || page.width() == null) {
                continue;
            }
            bodyPages.add(block.pageIndex());
            double width = block.bbox().get(2) - block.bbox().get(0);
            if (width >= page.width() * 0.62) {
                continue;
            }
            double center = (block.bbox().get(0) + block.bbox().get(2)) / 2.0 / page.width();
            boolean[] pageBands = bands.computeIfAbsent(block.pageIndex(), ignored -> new boolean[2]);
            if (center < 0.48) pageBands[0] = true;
            if (center > 0.52) pageBands[1] = true;
            if (pageBands[0] && pageBands[1]) twoColumnPages.add(block.pageIndex());
        }
        boolean twoColumn = !twoColumnPages.isEmpty()
                && twoColumnPages.size() * 2 >= Math.max(1, bodyPages.size());
        return new Layout(twoColumn, Map.copyOf(byPage), columnGapMm(blocks, byPage));
    }

    private static double columnGapMm(
            List<AiModelClient.ExtractionBlock> blocks,
            Map<Integer, AiModelClient.ExtractionPage> pages) {
        List<Double> gaps = new ArrayList<>();
        for (Integer pageIndex : pages.keySet()) {
            double width = pages.get(pageIndex).width();
            double leftEnd = blocks.stream()
                    .filter(block -> onPage(block, pageIndex) && center(block) < width / 2)
                    .mapToDouble(block -> block.bbox().get(2)).max().orElse(Double.NaN);
            double rightStart = blocks.stream()
                    .filter(block -> onPage(block, pageIndex) && center(block) > width / 2)
                    .mapToDouble(block -> block.bbox().get(0)).min().orElse(Double.NaN);
            if (Double.isFinite(leftEnd) && Double.isFinite(rightStart) && rightStart > leftEnd) {
                gaps.add((rightStart - leftEnd) / width * 210.0);
            }
        }
        return gaps.stream().mapToDouble(Double::doubleValue).average()
                .stream().map(value -> Math.max(4.0, Math.min(12.0, value))).findFirst().orElse(6.0);
    }

    private static boolean onPage(AiModelClient.ExtractionBlock block, int pageIndex) {
        return bodyBlock(block) && block.pageIndex() != null && block.pageIndex() == pageIndex
                && block.bbox() != null;
    }

    private static double center(AiModelClient.ExtractionBlock block) {
        return (block.bbox().get(0) + block.bbox().get(2)) / 2.0;
    }

    private static boolean bodyBlock(AiModelClient.ExtractionBlock block) {
        return block != null && !Set.of("header", "footer", "page_number").contains(block.type());
    }

    private static String preamble(Layout layout, List<AiModelClient.ExtractionBlock> blocks) {
        String options = layout.twoColumn()
                ? "a4paper,10pt,twoside,twocolumn"
                : "a4paper,10pt,twoside";
        List<String> runningHeaders = distinctFurniture(blocks, "header", false);
        List<String> firstHeaders = distinctFurniture(blocks, "header", true);
        List<String> runningFooters = distinctFurniture(blocks, "footer", false);
        List<String> firstFooters = distinctFurniture(blocks, "footer", true);
        int firstPage = startingPage(blocks);

        String leftHeader = runningHeaders.isEmpty() ? "" : runningHeaders.get(0);
        String rightHeader = runningHeaders.size() > 1 ? runningHeaders.get(1) : leftHeader;
        String footer = runningFooters.isEmpty() ? "" : runningFooters.get(0);
        String firstLeft = firstHeaders.isEmpty() ? leftHeader : firstHeaders.get(0);
        String firstRight = firstHeaders.size() > 1 ? firstHeaders.get(1) : "";
        String firstFooter = firstFooters.isEmpty() ? footer : firstFooters.get(0);

        return "\\documentclass[" + options + "]{article}\n"
                + "\\usepackage[utf8]{inputenc}\n"
                + "\\usepackage[T1]{fontenc}\n"
                + "\\usepackage[left=23mm,right=23mm,top=20mm,bottom=20mm]{geometry}\n"
                + "\\usepackage{graphicx,array,booktabs,tabularx,fancyhdr,xcolor,soul,hyperref}\n"
                + "\\graphicspath{{images/}}\n"
                + "\\newcommand{\\epclaim}[2]{#2}\n"
                + "\\setlength{\\columnsep}{" + String.format(Locale.ROOT, "%.1f", layout.columnGapMm()) + "mm}\n"
                + "\\setlength{\\headheight}{14pt}\n"
                + "\\pagestyle{fancy}\n\\fancyhf{}\n"
                + "\\fancyhead[LE,RO]{\\thepage}\n"
                + "\\fancyhead[LO]{" + escapeText(leftHeader) + "}\n"
                + "\\fancyhead[RE]{" + escapeText(rightHeader) + "}\n"
                + "\\fancyfoot[LE,RO]{" + escapeText(footer) + "}\n"
                + "\\fancypagestyle{firstpage}{\\fancyhf{}"
                + "\\fancyhead[L]{" + escapeText(firstLeft) + "}"
                + "\\fancyhead[R]{" + escapeText(firstRight) + "}"
                + "\\fancyfoot[L]{" + escapeText(firstFooter) + "}}\n"
                + "\\AtBeginDocument{\\setcounter{page}{" + firstPage + "}}";
    }

    private static List<String> distinctFurniture(
            List<AiModelClient.ExtractionBlock> blocks,
            String type,
            boolean firstPage) {
        return blocks.stream()
                .filter(block -> type.equals(block.type()) && block.pageIndex() != null
                        && (firstPage ? block.pageIndex() == 0 : block.pageIndex() > 0))
                .map(AiModelClient.ExtractionBlock::text)
                .filter(text -> text != null && !text.isBlank())
                .distinct()
                .limit(2)
                .toList();
    }

    private static int startingPage(List<AiModelClient.ExtractionBlock> blocks) {
        int start = Integer.MAX_VALUE;
        for (AiModelClient.ExtractionBlock block : blocks) {
            if (!"page_number".equals(block.type()) || block.pageIndex() == null) {
                continue;
            }
            Matcher matcher = PAGE_NUMBER.matcher(block.text());
            if (matcher.find()) {
                start = Math.min(start, Integer.parseInt(matcher.group()) - block.pageIndex());
            }
        }
        return start == Integer.MAX_VALUE ? 1 : Math.max(1, start);
    }

    private static String topLevelTitle(AiModelClient.ExtractionBlock block) {
        if (block == null || !("heading".equals(block.type()) || "reference".equals(block.type()))) {
            return null;
        }
        String stripped = stripHeadingNumber(block.text());
        String normalized = normalize(stripped);
        if (!TOP_LEVEL.contains(normalized)) {
            return null;
        }
        return switch (normalized) {
            case "references" -> "References";
            case "bibliography" -> "Bibliography";
            case "works cited" -> "Works Cited";
            default -> stripped;
        };
    }

    private static String stripHeadingNumber(String text) {
        return HEADING_NUMBER.matcher(text == null ? "" : text.strip()).replaceFirst("");
    }

    private static String normalize(String text) {
        return stripHeadingNumber(text).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String stripMathDelimiters(String text) {
        String stripped = text.strip();
        if (stripped.startsWith("$$") && stripped.endsWith("$$") && stripped.length() > 4) {
            return stripped.substring(2, stripped.length() - 2).strip();
        }
        return stripped;
    }

    static String escapeText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        Matcher matcher = HTML_SUPERSCRIPT.matcher(value);
        int start = 0;
        while (matcher.find()) {
            appendEscapedText(escaped, value, start, matcher.start());
            escaped.append("\\textsuperscript{");
            appendEscapedText(escaped, matcher.group(1), 0, matcher.group(1).length());
            escaped.append('}');
            start = matcher.end();
        }
        appendEscapedText(escaped, value, start, value.length());
        return escaped.toString();
    }

    static String normalizeLegacyMarkup(String value) {
        if (value == null) {
            return "";
        }
        Matcher matcher = HTML_SUPERSCRIPT.matcher(value);
        StringBuilder normalized = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(normalized, Matcher.quoteReplacement(
                    "\\textsuperscript{" + escapeText(matcher.group(1)) + "}"));
        }
        return matcher.appendTail(normalized).toString();
    }

    private static void appendEscapedText(
            StringBuilder escaped,
            String value,
            int start,
            int end) {
        for (int index = start; index < end; index++) {
            char character = value.charAt(index);
            escaped.append(switch (character) {
                case '\\' -> "\\textbackslash{}";
                case '&' -> "\\&";
                case '%' -> "\\%";
                case '$' -> "\\$";
                case '#' -> "\\#";
                case '_' -> "\\_";
                case '{' -> "\\{";
                case '}' -> "\\}";
                case '~' -> "\\textasciitilde{}";
                case '^' -> "\\textasciicircum{}";
                default -> String.valueOf(character);
            });
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    public record ImportedPaper(
            String preambleTex,
            String frontMatterTex,
            String title,
            String authors,
            List<SectionTex> sections) {
        public ImportedPaper {
            sections = List.copyOf(sections);
        }
    }

    public record SectionTex(String title, String contentTex) {
    }

    private record FrontMatter(String tex, String title, String authors) {
    }

    private record Layout(
            boolean twoColumn,
            Map<Integer, AiModelClient.ExtractionPage> pages,
            double columnGapMm) {
        boolean wide(AiModelClient.ExtractionBlock block) {
            if (block.pageIndex() == null || block.bbox() == null) {
                return false;
            }
            AiModelClient.ExtractionPage page = pages.get(block.pageIndex());
            return page != null && page.width() != null
                    && block.bbox().get(2) - block.bbox().get(0) >= page.width() * 0.72;
        }
    }
}
