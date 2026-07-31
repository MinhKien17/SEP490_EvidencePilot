package com.evidencepilot.service;

import com.evidencepilot.dto.response.ClaimConsistencyResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TexArchiveBuilderTest {

    @Test
    void writesStandardTemplateClaimMarkerAndVisibleWarnings() throws Exception {
        ProjectRepository projects = mock(ProjectRepository.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        PaperSectionRepository sections = mock(PaperSectionRepository.class);
        ClaimContentConsistencyService consistency =
                mock(ClaimContentConsistencyService.class);
        TexArchiveMediaWriter media = mock(TexArchiveMediaWriter.class);
        TexArchiveBuilder builder = new TexArchiveBuilder(
                projects,
                documents,
                sections,
                new PaperStandardService(),
                consistency,
                media);
        UUID projectId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        project.setTitle("AI_Project");
        project.setTargetStandard(PaperStandard.IEEE);
        Document paper = new Document();
        paper.setId(UUID.randomUUID());
        paper.setDocType(DocumentType.PAPER);
        paper.setActive(true);
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(paper);
        section.setSectionTitle("Introduction");
        section.setSectionOrder(0);
        section.setContentTex("\\epclaim{" + claimId + "}{Supported Claim}");
        section.setActive(true);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(documents.findByProjectIdAndDocTypeAndActiveTrue(
                projectId, DocumentType.PAPER)).thenReturn(List.of(paper));
        when(sections.findByDocumentIdOrderBySectionOrderAsc(paper.getId()))
                .thenReturn(List.of(section));
        when(consistency.preflight(projectId)).thenReturn(
                new ClaimConsistencyResponse(1, List.of(
                        new ClaimConsistencyResponse.Warning(
                                claimId,
                                section.getId(),
                                ClaimContentStatus.MISSING,
                                "Claim is saved but not used."))));
        var archive = Files.createTempFile("tex-builder-test-", ".zip");

        try {
            builder.write(projectId, archive);
            try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
                String main = text(zip, "main.tex");
                assertThat(main)
                        .contains("\\documentclass[conference]{IEEEtran}")
                        .contains("\\newcommand{\\epclaim}[2]{#2}")
                        .contains("\\title{AI\\_Project}")
                        .contains("\\input{sections/01-introduction.tex}");
                assertThat(text(zip, "sections/01-introduction.tex"))
                        .contains("\\epclaim{" + claimId + "}{Supported Claim}");
                assertThat(text(zip, "CLAIM_WARNINGS.md"))
                        .contains(claimId.toString(), "MISSING");
            }
            verify(media).writeProjectMedia(any(), any());
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    private static String text(ZipFile zip, String entry) throws Exception {
        return new String(
                zip.getInputStream(zip.getEntry(entry)).readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
