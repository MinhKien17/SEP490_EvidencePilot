package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ReviewSnapshot;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ReviewSnapshotRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.AuditService;
import com.evidencepilot.service.PaperStandardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SectionCitationReviewServiceTest {

    private final AiModelClient aiModelClient = mock(AiModelClient.class);
    private final PaperSectionRepository sectionRepository = mock(PaperSectionRepository.class);
    private final ReviewSnapshotRepository snapshotRepository = mock(ReviewSnapshotRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SourceMatchingService sourceMatchingService = mock(SourceMatchingService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void runPersistsGroundedSectionFinding() {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String excerpt = "Prior studies report a 42 percent improvement";
        PaperSection section = section(projectId, documentId, sectionId, "Introduction", excerpt + ".");
        User actor = new User();
        actor.setId(actorId);
        when(sectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        when(snapshotRepository.findByProjectIdAndStyleAndInputFingerprint(
                projectId, SectionCitationReviewService.REVIEW_VERSION, service().fingerprint(section)))
                .thenReturn(Optional.empty());
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(
                new AiModelClient.GenerationResult("provider", "model", """
                        {"summary":"One citation is needed.","findings":[{
                          "ruleCode":"QUANTITATIVE_OR_STATISTICAL_CLAIM",
                          "excerpt":"Prior studies report a 42 percent improvement",
                          "startOffset":0,
                          "endOffset":45,
                          "reason":"External quantitative claim.",
                          "recommendedAction":"Cite the supporting study."
                        }]}
                        """));

        SectionCitationReviewResponse result = service().run(
                documentId, projectId, sectionId, service().fingerprint(section), actorId);

        assertThat(result.complete()).isTrue();
        assertThat(result.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.excerpt()).isEqualTo(excerpt);
            assertThat(finding.startOffset()).isZero();
            assertThat(finding.endOffset()).isEqualTo(excerpt.length());
        });
        verify(snapshotRepository).save(any(ReviewSnapshot.class));
        verify(auditService).record(
                "AI_SECTION_CITATION_REVIEW", "PaperSection", sectionId, actor, null, result);
    }

    @Test
    void runRejectsStaleSectionFingerprintBeforeCallingAi() {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        PaperSection section = section(
                projectId, documentId, sectionId, "Introduction", "Saved content");
        when(sectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));

        assertThatThrownBy(() -> service().run(
                documentId, projectId, sectionId, "stale", UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(aiModelClient, never()).generate(anyString(), anyString());
    }

    @Test
    void fingerprintChangesWhenSectionTitleChanges() {
        PaperSection section = section(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Introduction", "The same saved content");
        String introductionFingerprint = service().fingerprint(section);

        section.setSectionTitle("Results");

        assertThat(service().fingerprint(section)).isNotEqualTo(introductionFingerprint);
    }

    private SectionCitationReviewService service() {
        return new SectionCitationReviewService(
                aiModelClient,
                sectionRepository,
                snapshotRepository,
                userRepository,
                new PaperStandardService(),
                sourceMatchingService,
                auditService,
                objectMapper);
    }

    private static PaperSection section(
            UUID projectId,
            UUID documentId,
            UUID sectionId,
            String title,
            String content) {
        Project project = new Project();
        project.setId(projectId);
        project.setTargetStandard(PaperStandard.IEEE);
        Document document = new Document();
        document.setId(documentId);
        document.setProject(project);
        document.setDocType(DocumentType.PAPER);
        document.setActive(true);
        PaperSection section = new PaperSection();
        section.setId(sectionId);
        section.setDocument(document);
        section.setSectionTitle(title);
        section.setContentTex(content);
        section.setVersion(2);
        section.setActive(true);
        return section;
    }
}
