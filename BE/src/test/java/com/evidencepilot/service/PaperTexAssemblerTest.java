package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperTexAssemblerTest {

    @Test
    void assemblesFullPaperAndAppliesOnlyUnsavedSectionOverride() throws Exception {
        Fixture fixture = fixture("Saved content");
        PaperTexAssembler.SectionDraft draft = new PaperTexAssembler.SectionDraft(
                fixture.section.getId(), "Unsaved content");

        PaperTexAssembler.PaperTexWorkspace workspace = fixture.assembler.assemble(
                fixture.paper.getId(), new PaperTexAssembler.DraftOverride(draft, null));
        var root = workspace.root();
        try (workspace) {
            assertThat(Files.readString(workspace.mainTex()))
                    .contains("\\input{preamble.tex}")
                    .contains("\\input{frontmatter.tex}")
                    .contains("\\input{paper-body.tex}");
            assertThat(Files.readString(root.resolve("preamble.tex")))
                    .contains("\\documentclass[conference]{IEEEtran}");
            String body = Files.readString(root.resolve("paper-body.tex"));
            String sectionFile = body.replace("\\input{", "").replace("}", "").strip();
            assertThat(Files.readString(root.resolve(sectionFile)))
                    .contains("\\section{Introduction}")
                    .contains("Unsaved content")
                    .doesNotContain("Saved content");
            verify(fixture.currentUserService).requireSectionContentWriteAccess(
                    fixture.user, fixture.section);
        }
        assertThat(root).doesNotExist();
    }

    @Test
    void rejectsParentTraversalBeforeStartingCompiler() {
        Fixture fixture = fixture("\\includegraphics{../secret.png}");

        assertThatThrownBy(() -> fixture.assembler.assemble(
                fixture.paper.getId(), new PaperTexAssembler.DraftOverride(null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("unsafe image path");
    }

    @Test
    void normalizesLegacySuperscriptMarkupInStoredFrontMatter() throws Exception {
        Fixture fixture = fixture("Saved content");
        fixture.paper.setFrontMatterTex("A. Author<sup>1</sup> \\& B. Author");

        PaperTexAssembler.PaperTexWorkspace workspace = fixture.assembler.assemble(
                fixture.paper.getId(), new PaperTexAssembler.DraftOverride(null, null));
        try (workspace) {
            assertThat(Files.readString(workspace.root().resolve("frontmatter.tex")))
                    .contains("A. Author\\textsuperscript{1} \\& B. Author")
                    .doesNotContain("<sup>");
        }
    }

    private static Fixture fixture(String content) {
        DocumentRepository documents = mock(DocumentRepository.class);
        PaperSectionRepository sections = mock(PaperSectionRepository.class);
        CurrentUserService current = mock(CurrentUserService.class);
        TexArchiveMediaWriter media = mock(TexArchiveMediaWriter.class);
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Project");
        project.setTargetStandard(PaperStandard.IEEE);
        Document paper = new Document();
        paper.setId(UUID.randomUUID());
        paper.setProject(project);
        paper.setDocType(DocumentType.PAPER);
        paper.setActive(true);
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(paper);
        section.setSectionTitle("Introduction");
        section.setSectionOrder(0);
        section.setContentTex(content);
        section.setActive(true);
        User user = new User();
        user.setId(UUID.randomUUID());
        when(documents.findById(paper.getId())).thenReturn(Optional.of(paper));
        when(sections.findByDocumentIdOrderBySectionOrderAsc(paper.getId()))
                .thenReturn(List.of(section));
        when(sections.findByIdWithDocument(section.getId())).thenReturn(Optional.of(section));
        when(current.requireCurrentUser()).thenReturn(user);
        PaperTexAssembler assembler = new PaperTexAssembler(
                documents,
                sections,
                current,
                new PaperTexSourceBuilder(new PaperStandardService()),
                media);
        return new Fixture(assembler, paper, section, user, current);
    }

    private record Fixture(
            PaperTexAssembler assembler,
            Document paper,
            PaperSection section,
            User user,
            CurrentUserService currentUserService) {
    }
}
