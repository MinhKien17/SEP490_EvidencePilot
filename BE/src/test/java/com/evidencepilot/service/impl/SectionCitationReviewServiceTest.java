package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ReviewSnapshot;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.prompt.SectionCitationReviewPrompt;
import com.evidencepilot.repository.DocumentChunkRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SectionCitationReviewServiceTest {

    private final AiModelClient aiModelClient = mock(AiModelClient.class);
    private final PaperSectionRepository sectionRepository = mock(PaperSectionRepository.class);
    private final ReviewSnapshotRepository snapshotRepository = mock(ReviewSnapshotRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SourceMatchingService sourceMatchingService = mock(SourceMatchingService.class);
    private final DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void runPersistsGroundedSourceDiscrepancyFinding() {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String excerpt = "Smith et al. report 92 percent accuracy on the benchmark";
        PaperSection section = section(projectId, documentId, sectionId, "Introduction", excerpt + ".");
        User actor = new User();
        actor.setId(actorId);
        when(sectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        DocumentChunk retrievedChunk = sourceChunk(sourceId, chunkId,
                "The final model achieved 89.2 percent accuracy on the benchmark evaluation.");
        when(sourceMatchingService.search(eq(projectId), any(), eq(5)))
                .thenReturn(List.of(List.of(
                        new SourceMatchingService.SourceMatch(retrievedChunk, 0.91f))));
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(
                new AiModelClient.GenerationResult("provider", "model", String.format("""
                        {"section_id":"%s","chunk_index":0,"findings":[{
                          "type":"SOURCE_DISCREPANCY",
                          "excerpt":"%s",
                          "start_offset":0,
                          "end_offset":%d,
                          "rationale":"The paper reports 92 percent but the cited source reports 89.2 percent.",
                          "confidence":"HIGH",
                          "evidence":[{"source_id":"%s","chunk_id":"%s",
                            "quote":"achieved 89.2 percent accuracy",
                            "relation":"CONTRADICTS"}]
                        }]}
                        """, sectionId, excerpt, excerpt.length(), sourceId, chunkId)));

        SectionCitationReviewResponse result = service().run(
                documentId, projectId, sectionId, service().fingerprint(section), actorId);

        assertThat(result.complete()).isTrue();
        assertThat(result.summary()).contains("1 source discrepanc");
        assertThat(result.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.type())
                    .isEqualTo(SectionCitationReviewResponse.FindingType.SOURCE_DISCREPANCY);
            assertThat(finding.excerpt()).isEqualTo(excerpt);
            assertThat(finding.startOffset()).isZero();
            assertThat(finding.endOffset()).isEqualTo(excerpt.length());
            assertThat(finding.confidence())
                    .isEqualTo(SectionCitationReviewResponse.Confidence.HIGH);
            assertThat(finding.evidence()).singleElement().satisfies(evidence -> {
                assertThat(evidence.sourceId()).isEqualTo(sourceId);
                assertThat(evidence.chunkId()).isEqualTo(chunkId);
                assertThat(evidence.quote()).isEqualTo("achieved 89.2 percent accuracy");
                assertThat(evidence.relation())
                        .isEqualTo(SectionCitationReviewResponse.EvidenceRelation.CONTRADICTS);
            });
        });
        verify(aiModelClient).generate(eq(SectionCitationReviewPrompt.SYSTEM), anyString());
        verify(snapshotRepository).save(any(ReviewSnapshot.class));
        verify(auditService).record(
                "AI_SECTION_CITATION_REVIEW", "PaperSection", sectionId, actor, null, result);
    }

    @Test
    void runRejectsUnsubstantiatedClaimCarryingSupportingEvidence() {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String excerpt = "Our method improves recall by 34 percent over prior work";
        PaperSection section = section(projectId, documentId, sectionId, "Introduction", excerpt + ".");
        when(sectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        DocumentChunk supportingChunk = sourceChunk(sourceId, chunkId,
                "Recall improved by 34 percent in the reported experiments.");
        when(sourceMatchingService.search(eq(projectId), any(), eq(5)))
                .thenReturn(List.of(List.of(
                        new SourceMatchingService.SourceMatch(supportingChunk, 0.88f))));
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(
                new AiModelClient.GenerationResult("provider", "model", String.format("""
                        {"section_id":"%s","chunk_index":0,"findings":[{
                          "type":"UNSUBSTANTIATED_CLAIM",
                          "excerpt":"%s",
                          "start_offset":0,
                          "end_offset":%d,
                          "rationale":"Empirical claim without citation.",
                          "confidence":"MEDIUM",
                          "evidence":[{"source_id":"%s","chunk_id":"%s",
                            "quote":"Recall improved by 34 percent",
                            "relation":"SUPPORTS"}]
                        }]}
                        """, sectionId, excerpt, excerpt.length(), sourceId, chunkId)));

        assertThatThrownBy(() -> service().run(
                documentId, projectId, sectionId, service().fingerprint(section), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(aiModelClient, times(2)).generate(anyString(), anyString());
    }

    @Test
    void runExemptsAbstractWithoutCallingAi() {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        PaperSection section = section(
                projectId, documentId, sectionId, "Abstract",
                "This paper presents a retrieval-augmented critique pipeline.");
        User actor = new User();
        actor.setId(actorId);
        when(sectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        SectionCitationReviewResponse result = service().run(
                documentId, projectId, sectionId, service().fingerprint(section), actorId);

        assertThat(result.complete()).isTrue();
        assertThat(result.findings()).isEmpty();
        assertThat(result.summary()).contains("exempt");
        verify(aiModelClient, never()).generate(anyString(), anyString());
        verify(sourceMatchingService, never()).search(any(), any(), anyInt());
        verify(snapshotRepository).save(any(ReviewSnapshot.class));
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

    @Test
    void fingerprintChangesWhenSourceCorpusChanges() {
        UUID projectId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        PaperSection section = section(
                projectId, UUID.randomUUID(), UUID.randomUUID(),
                "Introduction", "The same saved content");
        String emptyCorpusFingerprint = service().fingerprint(section);

        Document source = new Document();
        source.setId(sourceId);
        DocumentChunk corpusChunk = sourceChunk(sourceId, UUID.randomUUID(), "chunk text");
        when(sourceMatchingService.activeSources(projectId)).thenReturn(List.of(source));
        when(documentChunkRepository.findByDocumentId(sourceId))
                .thenReturn(List.of(corpusChunk));

        assertThat(service().fingerprint(section)).isNotEqualTo(emptyCorpusFingerprint);
    }

    private SectionCitationReviewService service() {
        return new SectionCitationReviewService(
                aiModelClient,
                sectionRepository,
                snapshotRepository,
                userRepository,
                new PaperStandardService(),
                sourceMatchingService,
                documentChunkRepository,
                auditService,
                objectMapper);
    }

    private static DocumentChunk sourceChunk(UUID sourceId, UUID chunkId, String text) {
        Document source = new Document();
        source.setId(sourceId);
        source.setTitle("Smith et al. 2024");
        source.setOriginalFilename("smith-et-al.pdf");
        DocumentChunk chunk = mock(DocumentChunk.class);
        when(chunk.getId()).thenReturn(chunkId);
        when(chunk.getText()).thenReturn(text);
        when(chunk.getDocument()).thenReturn(source);
        return chunk;
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
