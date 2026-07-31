package com.evidencepilot.service;

import com.evidencepilot.model.enums.PaperStandard;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaperStandardServiceTest {

    private final PaperStandardService service = new PaperStandardService();

    @Test
    void everyPaperStandardHasAResolvableTexTemplate() {
        for (PaperStandard standard : PaperStandard.values()) {
            String rendered = service.renderTemplate(
                    standard, "Project", "\\input{sections/01-introduction.tex}");
            assertThat(rendered)
                    .doesNotContain("{{TITLE}}", "{{BODY}}")
                    .contains("\\newcommand{\\epclaim}[2]{#2}")
                    .contains("\\input{sections/01-introduction.tex}");
        }
    }

    @Test
    void templateCommentsDoNotCountAsStudentContent() {
        String guidance = service.getSectionTemplate(
                PaperStandard.IEEE, "Introduction");
        assertThat(service.hasStudentContent(guidance)).isFalse();
        assertThat(service.hasStudentContent(guidance + "\nActual paper text.")).isTrue();
    }
}
