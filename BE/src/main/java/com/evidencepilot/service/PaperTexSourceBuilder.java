package com.evidencepilot.service;

import com.evidencepilot.model.enums.PaperStandard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaperTexSourceBuilder {

    private static final String BODY_MARKER = "%% EVIDENCEPILOT_BODY %%";
    private static final String BEGIN_DOCUMENT = "\\begin{document}";

    private final PaperStandardService paperStandardService;

    public Shell resolveShell(
            PaperStandard standard,
            String title,
            String preambleTex,
            String frontMatterTex) {
        String template = paperStandardService.renderTemplate(
                standard, escapeLatex(title), BODY_MARKER);
        int begin = template.indexOf(BEGIN_DOCUMENT);
        int body = template.indexOf(BODY_MARKER);
        if (begin < 0 || body < begin) {
            throw new IllegalStateException("Paper template is missing document markers");
        }
        String defaultPreamble = template.substring(0, begin).strip();
        String defaultFrontMatter = template.substring(begin + BEGIN_DOCUMENT.length(), body).strip();
        String resolvedFrontMatter = frontMatterTex == null || frontMatterTex.isBlank()
                ? defaultFrontMatter : frontMatterTex.strip();
        return new Shell(
                preambleTex == null || preambleTex.isBlank() ? defaultPreamble : preambleTex.strip(),
                ImportedPaperTexBuilder.normalizeLegacyMarkup(resolvedFrontMatter));
    }

    public String renderInline(Shell shell, String body) {
        return shell.preambleTex() + "\n" + BEGIN_DOCUMENT + "\n"
                + shell.frontMatterTex() + "\n\n" + body.strip()
                + "\n\\end{document}\n";
    }

    public String generatedMain() {
        return "\\input{preamble.tex}\n"
                + BEGIN_DOCUMENT + "\n"
                + "\\input{frontmatter.tex}\n"
                + "\\input{paper-body.tex}\n"
                + "\\end{document}\n";
    }

    static String escapeLatex(String value) {
        if (value == null) {
            return "Untitled";
        }
        return value.replace("\\", "\\textbackslash{}")
                .replace("&", "\\&").replace("%", "\\%")
                .replace("$", "\\$").replace("#", "\\#")
                .replace("_", "\\_").replace("{", "\\{")
                .replace("}", "\\}").replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");
    }

    public record Shell(String preambleTex, String frontMatterTex) {
    }
}
