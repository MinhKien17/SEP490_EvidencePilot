package com.evidencepilot.service.impl;

import com.evidencepilot.client.ai.config.SectionAuditProperties;
import com.evidencepilot.dto.ai.AuditedSnippet;
import com.evidencepilot.dto.ai.SectionContextRequest;
import com.evidencepilot.dto.ai.SectionContextResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.SectionAuditFinding;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.SectionAuditFindingStatus;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.SectionAuditFindingRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.AuditService;
import com.evidencepilot.service.PaperStandardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SectionAuditServiceTest {

    private final AiModelClient aiModelClient = mock(AiModelClient.class);
    private final PaperSectionRepository paperSectionRepository = mock(PaperSectionRepository.class);
    private final SectionAuditFindingRepository repository = mock(SectionAuditFindingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PaperStandardService paperStandardService = mock(PaperStandardService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final SectionAuditProperties properties = new SectionAuditProperties();
    private final SectionAuditService service = new SectionAuditService(
            aiModelClient, paperSectionRepository, repository, userRepository,
            paperStandardService, auditService, properties, new Semaphore(4));

    private final UUID projectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final PaperSection section = new PaperSection();
    private final User user = new User();

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setId(projectId);
        Document document = new Document();
        document.setProject(project);
        document.setActive(true);
        document.setDocType(DocumentType.PAPER);
        section.setId(sectionId);
        section.setDocument(document);
        section.setActive(true);
        section.setSectionTitle("Introduction");
        section.setVersion(1);
        user.setId(UUID.randomUUID());
        when(paperSectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        when(paperSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(paperStandardService.normalizeSectionTitle(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.findBySectionIdAndContentFingerprint(any(), any())).thenReturn(List.of());
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void auditReanchorsChunkOffsetsToSectionAbsoluteAndPersists() throws Exception {
        section.setContentTex("First paragraph.\n\nSecond paragraph with risk.");
        String expectedFingerprint = SectionAuditService.fingerprint(section.getContentTex());
        when(aiModelClient.auditSection(any())).thenAnswer(inv -> {
            SectionContextRequest request = inv.getArgument(0);
            if (request.sourceChunk().startsWith("Second")) {
                return new SectionContextResponse(List.of(new AuditedSnippet(
                        "Second paragraph", 0, 16, "PARAPHRASE_RISK", "near-verbatim", "rephrased")));
            }
            return new SectionContextResponse(List.of());
        });

        var response = service.run(projectId, sectionId, expectedFingerprint, user.getId());

        verify(aiModelClient, org.mockito.Mockito.times(2)).auditSection(any());
        assertThat(response.findings()).hasSize(1);
        assertThat(response.findings().getFirst().startIndex()).isEqualTo(18);
        assertThat(response.findings().getFirst().endIndex()).isEqualTo(34);
        assertThat(response.findings().getFirst().originalTextSnippet()).isEqualTo("Second paragraph");
        assertThat(response.findings().getFirst().issueType())
                .isEqualTo(com.evidencepilot.model.enums.SectionAuditIssueType.PARAPHRASE_RISK);
        assertThat(response.findings().getFirst().status()).isEqualTo(SectionAuditFindingStatus.PENDING);
    }

    @Test
    void auditSplitsOversizedParagraphsOnSentenceBoundaries() {
        section.setContentTex("Sentence one here. ".repeat(600));
        String expectedFingerprint = SectionAuditService.fingerprint(section.getContentTex());
        List<String> chunkContents = new ArrayList<>();
        when(aiModelClient.auditSection(any())).thenAnswer(inv -> {
            chunkContents.add(((SectionContextRequest) inv.getArgument(0)).sourceChunk());
            return new SectionContextResponse(List.of());
        });

        service.run(projectId, sectionId, expectedFingerprint, user.getId());

        assertThat(chunkContents).hasSize(2);
        assertThat(chunkContents).allSatisfy(chunk -> {
            assertThat(chunk.length()).isLessThanOrEqualTo(8_000);
            assertThat(chunk).isNotBlank();
            assertThat(chunk).contains("Sentence one here.");
        });
    }

    @Test
    void auditReturnsCachedFindingsWithoutAiCall() {
        section.setContentTex("Unchanged content.");
        SectionAuditFinding cached = new SectionAuditFinding();
        cached.setId(UUID.randomUUID());
        cached.setSection(section);
        cached.setStatus(SectionAuditFindingStatus.RESOLVED);
        cached.setStartIndex(0);
        cached.setEndIndex(10);
        when(repository.findBySectionIdAndContentFingerprint(
                sectionId, SectionAuditService.fingerprint("Unchanged content.")))
                .thenReturn(List.of(cached));

        var response = service.run(
                projectId, sectionId, SectionAuditService.fingerprint("Unchanged content."), user.getId());

        assertThat(response.findings()).hasSize(1);
        assertThat(response.findings().getFirst().status()).isEqualTo(SectionAuditFindingStatus.RESOLVED);
        verify(aiModelClient, never()).auditSection(any());
    }

    @Test
    void auditFailsWithoutPersistingWhenAiOutputIsNotGrounded() {
        section.setContentTex("Some clean content here.");
        when(aiModelClient.auditSection(any())).thenReturn(new SectionContextResponse(List.of(
                new AuditedSnippet("invented text", 0, 5, "PARAPHRASE_RISK", "nope", null))));

        assertThatThrownBy(() -> service.run(
                projectId, sectionId, SectionAuditService.fingerprint(section.getContentTex()), user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(502));

        verify(repository, never()).saveAll(any());
    }

    @Test
    void auditReanchorsPythonCodePointOffsetsViaIndexOfFallback() {
        // ponytail: "😀" is one code point but two UTF-16 units; a Python-audited
        // snippet indexed by code points must be re-anchored against raw text.
        section.setContentTex("a😀b risky text");
        when(aiModelClient.auditSection(any())).thenReturn(new SectionContextResponse(List.of(
                new AuditedSnippet("risky text", 3, 13, "PARAPHRASE_RISK", "near-verbatim", null))));

        var response = service.run(
                projectId, sectionId, SectionAuditService.fingerprint(section.getContentTex()), user.getId());

        assertThat(response.findings()).hasSize(1);
        assertThat(response.findings().getFirst().startIndex()).isEqualTo(5);
        assertThat(response.findings().getFirst().endIndex()).isEqualTo(15);
        assertThat(response.findings().getFirst().originalTextSnippet()).isEqualTo("risky text");
    }

    @Test
    void runRejectsWhenContentChangedSinceSubmission() {
        section.setContentTex("New content.");
        assertThatThrownBy(() -> service.run(
                projectId, sectionId, SectionAuditService.fingerprint("Old content"), user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));

        verify(aiModelClient, never()).auditSection(any());
    }

    @Test
    void runAppendsNewFindingsKeepingStaleRows() {
        section.setContentTex("Changed content.");
        when(aiModelClient.auditSection(any())).thenReturn(new SectionContextResponse(List.of(
                new AuditedSnippet("Changed", 0, 7, "PARAPHRASE_RISK", "near-verbatim", null))));

        service.run(projectId, sectionId, SectionAuditService.fingerprint("Changed content."), user.getId());

        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(rows ->
                ((List<?>) rows).size() == 1
                        && ((SectionAuditFinding) ((List<?>) rows).get(0)).getContentFingerprint()
                                .equals(SectionAuditService.fingerprint("Changed content."))));
    }

    @Test
    void findingsFiltersOutStaleFingerprintRows() {
        section.setContentTex("Current content.");
        SectionAuditFinding stale = new SectionAuditFinding();
        stale.setId(UUID.randomUUID());
        stale.setSection(section);
        stale.setStatus(SectionAuditFindingStatus.PENDING);
        stale.setStartIndex(0);
        stale.setEndIndex(5);
        SectionAuditFinding current = new SectionAuditFinding();
        current.setId(UUID.randomUUID());
        current.setSection(section);
        current.setStatus(SectionAuditFindingStatus.PENDING);
        current.setStartIndex(0);
        current.setEndIndex(7);
        when(repository.findBySectionIdAndContentFingerprintOrderByStartIndexAsc(
                sectionId, SectionAuditService.fingerprint("Current content.")))
                .thenReturn(List.of(current));

        var rows = service.findings(sectionId, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().id()).isEqualTo(current.getId());
    }

    @Test
    void updateStatusEnforcesPendingOnlyTransitions() {
        SectionAuditFinding finding = new SectionAuditFinding();
        finding.setId(UUID.randomUUID());
        finding.setSection(section);
        finding.setStartIndex(0);
        finding.setEndIndex(5);
        finding.setOriginalTextSnippet("snippet");
        finding.setIssueType(com.evidencepilot.model.enums.SectionAuditIssueType.PARAPHRASE_RISK);
        finding.setRationale("reason");
        finding.setStatus(SectionAuditFindingStatus.PENDING);
        finding.setCreatedAt(java.time.LocalDateTime.now());
        when(repository.findById(finding.getId())).thenReturn(Optional.of(finding));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resolved = service.updateStatus(finding.getId(), SectionAuditFindingStatus.RESOLVED, user.getId());
        assertThat(resolved.status()).isEqualTo(SectionAuditFindingStatus.RESOLVED);

        assertThatThrownBy(() -> service.updateStatus(finding.getId(), SectionAuditFindingStatus.PENDING, user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(400));

        finding.setStatus(SectionAuditFindingStatus.RESOLVED);
        assertThatThrownBy(() -> service.updateStatus(finding.getId(), SectionAuditFindingStatus.DISMISSED, user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void fingerprintIsStableSha256Hex() {
        assertThat(SectionAuditService.fingerprint("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
