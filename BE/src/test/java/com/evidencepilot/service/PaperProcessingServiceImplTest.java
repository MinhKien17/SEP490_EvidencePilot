package com.evidencepilot.service;

import com.evidencepilot.dto.response.AiReviewResponse;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ReviewSnapshot;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.ReviewSnapshotRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.PaperProcessingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PaperProcessingServiceImplTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private PaperSectionRepository paperSectionRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimEvidenceMappingRepository claimEvidenceMappingRepository;

    @Mock
    private InstructorFeedbackRepository instructorFeedbackRepository;

    @Mock
    private ClaimContentConsistencyService claimContentConsistencyService;

    @Mock
    private EvidenceFilterService evidenceFilterService;

    @Mock
    private AuditService auditService;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PaperStandardService paperStandardService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SystemNotificationService systemNotificationService;

    @Mock
    private TexArchiveBuilder texArchiveBuilder;

    @Mock
    private ReviewSnapshotRepository reviewSnapshotRepository;

    @Test
    void getPaperSectionsRequiresProjectAccess() {
        User user = user();
        Project project = project();
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of());

        service().getPaperSections(document.getId());

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void getPaperSectionsHidesSoftDeletedSections() {
        User user = user();
        Document document = document(project());
        PaperSection active = section(document);
        PaperSection deleted = section(document);
        deleted.setActive(false);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(active, deleted));

        service().getPaperSections(document.getId());

        verify(projectMapper).toPaperSectionResponse(active);
        verify(projectMapper, never()).toPaperSectionResponse(deleted);
    }

    @Test
    void reviewUsesCurrentSectionsEligibleEvidenceAndSectionFeedback() {
        User user = user();
        Project project = project();
        Document document = document(project);
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("Stale extracted text");
        document.setDocumentText(text);
        PaperSection section = section(document);
        section.setContentTex("Latest saved section content");
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("Latest saved section content");
        claim.setActive(true);
        ClaimEvidenceMapping eligible = mapping("Verified source snippet", MappingStatus.ACTIVE);
        ClaimEvidenceMapping excluded = mapping("Rejected source snippet", MappingStatus.INACTIVE);
        InstructorFeedback feedback = new InstructorFeedback();
        feedback.setId(UUID.randomUUID());
        feedback.setSection(section);
        feedback.setContent("Clarify this claim.");

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of(feedback));
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(claim));
        when(evidenceFilterService.activeMappings(claim)).thenReturn(List.of(eligible));
        when(claimContentConsistencyService.evaluate(claim)).thenReturn(ClaimContentStatus.PRESENT);
        when(aiModelClient.generate(anyString(), anyString()))
                .thenAnswer(generationAnswers());

        var response = service().review(document.getId(), null);

        verify(currentUserService).requireProjectAccess(user, project);
        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).generate(system.capture(), prompt.capture());
        assertThat(system.getValue())
                .contains("Review the supplied paper data")
                .doesNotContain("Latest saved section content");
        assertThat(prompt.getValue())
                .contains("Latest saved section content")
                .contains(eligible.getDocumentChunk().getDocument().getId().toString())
                .contains("Clarify this claim.")
                .doesNotContain("Stale extracted text")
                .doesNotContain("Verified source snippet")
                .doesNotContain("Rejected source snippet");
        assertThat(response.complete()).isTrue();
        assertThat(response.coverage().sectionsScanned()).isEqualTo(1);
        assertThat(response.summary()).contains("checked 1/1 active Claims");
    }

    @Test
    void backgroundReviewRejectsDocumentFromAnotherProject() {
        Project project = project();
        Document document = document(project);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service().runReview(
                document.getId(), UUID.randomUUID(), null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void backgroundReviewDoesNotRequireSecurityContext() {
        User requester = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenReturn(generated(validReviewJson()));

        AiReviewResponse response = service().runReview(
                document.getId(), project.getId(), null, requester.getId());

        assertThat(response.complete()).isTrue();
        org.mockito.Mockito.verifyNoInteractions(currentUserService);
    }

    @Test
    void reviewRetriesInvalidJsonOnce() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenReturn(generated("not-json"));

        assertThatThrownBy(() -> service().review(document.getId(), null))
                .isInstanceOf(com.evidencepilot.exception.AiValidationException.class)
                .hasMessageContaining("invalid findings JSON");
        ArgumentCaptor<String> systems = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient, times(2)).generate(systems.capture(), prompts.capture());
        assertThat(systems.getAllValues().get(0)).doesNotContain("previous response was invalid");
        assertThat(systems.getAllValues().get(1)).contains("previous response was invalid");
        assertThat(prompts.getAllValues()).containsOnly(prompts.getAllValues().get(0));
    }

    @Test
    void reviewSurfacesProviderRateLimitWithoutRetry() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenThrow(new AiModelClient.AiApiException("/ai/generate", 429));

        assertThatThrownBy(() -> service().review(document.getId(), null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(error.getReason()).contains("rate limit");
                });
        verify(aiModelClient).generate(anyString(), anyString());
    }

    @Test
    void reviewPreservesBadGatewayAfterRetry() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenThrow(new AiModelClient.AiApiException("/ai/generate", 502));

        assertThatThrownBy(() -> service().review(document.getId(), null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(error.getReason()).contains("invalid response");
                });
        verify(aiModelClient, times(2)).generate(anyString(), anyString());
    }

    @Test
    void reviewMapsNullAiJsonToValidationFailureAfterRetry() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenReturn(generated("null"));

        assertThatThrownBy(() -> service().review(document.getId(), null))
                .isInstanceOf(com.evidencepilot.exception.AiValidationException.class)
                .hasMessageContaining("invalid findings JSON");
        verify(aiModelClient, times(2)).generate(anyString(), anyString());
    }

    @Test
    void reviewMapsNullFindingJsonToValidationFailureAfterRetry() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenReturn(generated("null"));

        assertThatThrownBy(() -> service().review(document.getId(), null))
                .isInstanceOf(com.evidencepilot.exception.AiValidationException.class)
                .hasMessageContaining("invalid findings JSON");
        verify(aiModelClient, times(2)).generate(anyString(), anyString());
    }

    @Test
    void reviewRejectsMissingFindingsArrayAfterRetry() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenReturn(generated("{}"));

        assertThatThrownBy(() -> service().review(document.getId(), null))
                .isInstanceOf(com.evidencepilot.exception.AiValidationException.class)
                .hasMessageContaining("invalid findings JSON");
        verify(aiModelClient, times(2)).generate(anyString(), anyString());
    }

    @Test
    void reviewWithoutSectionContentIsIncompleteAndUnscored() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        section.setContentTex("   ");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());

        var response = service().review(document.getId(), null);

        assertThat(response.complete()).isFalse();
        assertThat(response.direction()).isEqualTo(AiReviewResponse.Direction.INSUFFICIENT_DATA);
        assertThat(response.rubricScore()).isNull();
        assertThat(response.passes()).isFalse();
        verify(aiModelClient, never()).generate(anyString(), anyString());
    }

    @Test
    void reviewWithBlankSectionReportsPartialCoverageAndNoScore() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection populated = section(document);
        PaperSection blank = section(document);
        blank.setContentTex("  ");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(populated, blank));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenAnswer(generationAnswers());

        var response = service().review(document.getId(), null);

        assertThat(response.complete()).isFalse();
        assertThat(response.direction()).isEqualTo(AiReviewResponse.Direction.INSUFFICIENT_DATA);
        assertThat(response.coverage().totalSections()).isEqualTo(2);
        assertThat(response.coverage().sectionsScanned()).isEqualTo(1);
        assertThat(response.reviewVersion()).isEqualTo("paper-claim-review-v8");
        assertThat(response.summary()).contains("Scanned 1/2 Sections");
        assertThat(response.rubricScore()).isNull();
        assertThat(response.passes()).isFalse();
    }

    @Test
    void reviewScansEveryPaperChunkWithoutSilentTruncation() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        section.setContentTex("x".repeat(8_100) + "TAIL_MARKER");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenAnswer(generationAnswers());

        var response = service().review(document.getId(), "IEEE");

        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).generate(anyString(), prompts.capture());
        assertThat(prompts.getValue()).contains("TAIL_MARKER");
        assertThat(response.coverage().totalChunks()).isEqualTo(2);
        assertThat(response.coverage().chunksScanned()).isEqualTo(2);
    }

    @Test
    void reviewFlagsAssertionWithNoMatchingClaim() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        section.setSectionTitle("Methodology");
        section.setContentTex("The system processes documents asynchronously.");
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("Evidence is stored in object storage.");
        claim.setActive(true);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(claim));
        when(evidenceFilterService.activeMappings(claim)).thenReturn(List.of());
        when(claimContentConsistencyService.evaluate(claim)).thenReturn(ClaimContentStatus.PRESENT);
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(generated("""
                {"summary":"A Claim is missing.","findings":[{
                  "type":"MISSING_CLAIM","severity":"WARNING","claimId":null,
                  "sectionId":"%s","sourceIds":[],"feedbackIds":[],
                  "excerpt":"The system processes documents asynchronously.",
                  "message":"Assertion has no corresponding Claim.",
                  "recommendedAction":"Create a Claim and attach evidence."
                }]}
                """.formatted(section.getId())));

        var response = service().review(document.getId(), null);

        assertThat(response.findings())
                .anyMatch(finding -> finding.type()
                        == com.evidencepilot.dto.response.AiReviewResponse.FindingType.MISSING_CLAIM
                        && section.getId().equals(finding.sectionId())
                        && finding.excerpt().contains("asynchronously"));
        verify(aiModelClient, never()).generateEmbeddings(any());
    }

    @Test
    void reviewDropsHallucinatedReferencesWithoutRetryingValidFindings() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(generated("""
                {"summary":"Review complete.","findings":[{
                  "type":"MISSING_CLAIM","severity":"WARNING","claimId":null,
                  "sectionId":"%s","sourceIds":["%s"],"feedbackIds":[],
                  "excerpt":"A result.","message":"The result has no Claim.",
                  "recommendedAction":"Create a Claim."
                }]}
                """.formatted(section.getId(), section.getId())));

        AiReviewResponse response = service().review(document.getId(), null);

        assertThat(response.findings()).singleElement()
                .satisfies(finding -> assertThat(finding.sourceIds()).isEmpty());
        verify(aiModelClient).generate(anyString(), anyString());
    }

    @Test
    void reviewDoesNotFlagAssertionCoveredByMatchingClaim() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        section.setContentTex("The system processes documents asynchronously.");
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("The system processes documents asynchronously.");
        claim.setActive(true);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(claim));
        when(evidenceFilterService.activeMappings(claim)).thenReturn(List.of());
        when(claimContentConsistencyService.evaluate(claim)).thenReturn(ClaimContentStatus.PRESENT);
        when(aiModelClient.generate(anyString(), anyString()))
                .thenReturn(generated(validReviewJson()));

        var response = service().review(document.getId(), null);

        assertThat(response.findings())
                .noneMatch(finding -> finding.type()
                        == com.evidencepilot.dto.response.AiReviewResponse.FindingType.MISSING_CLAIM);
        verify(aiModelClient, never()).generateEmbeddings(any());
    }

    @Test
    void reviewReusesCachedSnapshotWithoutAiCalls() throws Exception {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        AiReviewResponse cached = new AiReviewResponse(
                "paper-claim-review-v3", true,
                new AiReviewResponse.Coverage(1, 1, 1, 1, 0, 0),
                AiReviewResponse.Direction.ON_TRACK,
                "Cached summary", List.of(), List.of());
        ReviewSnapshot snapshot = new ReviewSnapshot();
        snapshot.setResponseJson(objectMapper.writeValueAsString(cached));
        when(reviewSnapshotRepository.findByProjectIdAndStyleAndInputFingerprint(
                eq(project.getId()), eq("default"), anyString())).thenReturn(Optional.of(snapshot));

        var response = service().review(document.getId(), null);

        assertThat(response.summary()).isEqualTo("Cached summary");
        verify(aiModelClient, never()).generate(anyString(), anyString());
        verify(reviewSnapshotRepository, never()).save(any());
    }

    @Test
    void reviewCacheLookupFailureFallsBackToFreshReview() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(reviewSnapshotRepository.findByProjectIdAndStyleAndInputFingerprint(
                eq(project.getId()), eq("default"), anyString()))
                .thenThrow(new RuntimeException("db down"));
        when(aiModelClient.generate(anyString(), anyString()))
                .thenAnswer(generationAnswers());
        when(reviewSnapshotRepository.save(any())).thenThrow(new RuntimeException("db down"));

        var response = service().review(document.getId(), null);

        assertThat(response.complete()).isTrue();
        verify(aiModelClient).generate(anyString(), anyString());
    }

    @Test
    void reviewDropsAiEmittedDeterministicAndUngroundedOtherFindings() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("Claim text present in the section content.");
        claim.setActive(true);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(claim));
        when(evidenceFilterService.activeMappings(claim)).thenReturn(List.of());
        when(claimContentConsistencyService.evaluate(claim)).thenReturn(ClaimContentStatus.PRESENT);
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(generated("""
                    {
                      "summary":"Claim review",
                      "findings":[
                        {"type":"UNUSED_CLAIM","severity":"WARNING","claimId":"%s",
                         "sectionId":"%s","sourceIds":[],"feedbackIds":[],"excerpt":"",
                         "message":"AI duplicate","recommendedAction":"ignore"},
                        {"type":"REDUNDANT_CLAIM","severity":"WARNING","claimId":"%s",
                         "sectionId":"%s","sourceIds":[],"feedbackIds":[],"excerpt":"",
                         "message":"Duplicate claim","recommendedAction":"keep one"},
                        {"type":"OTHER","severity":"WARNING","claimId":null,
                         "sectionId":"%s","sourceIds":[],"feedbackIds":[],"excerpt":"typo",
                         "message":"General writing issue","recommendedAction":"rewrite"}
                      ]
                    }
                    """.formatted(
                            claim.getId(), section.getId(), claim.getId(), section.getId(),
                            section.getId())));

        var response = service().review(document.getId(), null);

        assertThat(response.findings())
                .noneMatch(finding -> finding.type()
                        == com.evidencepilot.dto.response.AiReviewResponse.FindingType.UNUSED_CLAIM)
                .noneMatch(finding -> finding.type()
                        == com.evidencepilot.dto.response.AiReviewResponse.FindingType.OTHER)
                .anyMatch(finding -> finding.type()
                        == com.evidencepilot.dto.response.AiReviewResponse.FindingType.REDUNDANT_CLAIM);
    }

    @Test
    void reviewKeepsSemanticDuplicateButDropsAiCopyOfExactDuplicate() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        Claim first = claim(project, section, "The model improves retrieval accuracy.");
        Claim exact = claim(project, section, "  the model improves retrieval accuracy.  ");
        Claim semantic = claim(project, section, "Retrieval results become more accurate.");
        ClaimEvidenceMapping activeMapping = mapping("not sent to review", MappingStatus.ACTIVE);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId()))
                .thenReturn(List.of(first, exact, semantic));
        when(evidenceFilterService.activeMappings(any(Claim.class)))
                .thenReturn(List.of(activeMapping));
        when(claimContentConsistencyService.evaluate(any(Claim.class)))
                .thenReturn(ClaimContentStatus.PRESENT);
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(generated("""
                {"summary":"Duplicate review","findings":[
                  {"type":"REDUNDANT_CLAIM","severity":"WARNING","claimId":"%s",
                   "sectionId":"%s","sourceIds":[],"feedbackIds":[],"excerpt":"",
                   "message":"AI repeats the exact duplicate.","recommendedAction":"Keep one."},
                  {"type":"REDUNDANT_CLAIM","severity":"WARNING","claimId":"%s",
                   "sectionId":"%s","sourceIds":[],"feedbackIds":[],"excerpt":"",
                   "message":"This Claim has the same meaning.","recommendedAction":"Merge them."}
                ]}
                """.formatted(
                        exact.getId(), section.getId(), semantic.getId(), section.getId())));

        AiReviewResponse response = service().review(document.getId(), null);

        assertThat(response.findings().stream()
                .filter(finding -> finding.type() == AiReviewResponse.FindingType.REDUNDANT_CLAIM)
                .toList())
                .hasSize(2)
                .anyMatch(finding -> exact.getId().equals(finding.claimId())
                        && finding.message().contains("duplicates stored Claim"))
                .anyMatch(finding -> semantic.getId().equals(finding.claimId())
                        && finding.message().contains("same meaning"));
    }

    @Test
    void reviewCapsPrioritizedAiFindingsButKeepsDeterministicFindings() throws Exception {
        User user = user();
        Project project = project();
        Document document = document(project);
        List<PaperSection> sections = new ArrayList<>();
        List<AiReviewResponse.Finding> generatedFindings = new ArrayList<>();
        for (int index = 0; index < 17; index++) {
            PaperSection section = section(document);
            section.setSectionOrder(index);
            section.setSectionTitle("Section " + index);
            sections.add(section);
            generatedFindings.add(finding(
                    AiReviewResponse.FindingType.CLAIM_GAP,
                    AiReviewResponse.Severity.INFO,
                    section,
                    "Info " + index));
            generatedFindings.add(finding(
                    AiReviewResponse.FindingType.UNNECESSARY_CLAIM,
                    AiReviewResponse.Severity.WARNING,
                    section,
                    "Unnecessary " + index));
            generatedFindings.add(finding(
                    AiReviewResponse.FindingType.CLAIM_GAP,
                    AiReviewResponse.Severity.CRITICAL,
                    section,
                    "Critical " + index));
            generatedFindings.add(finding(
                    AiReviewResponse.FindingType.MISSING_CLAIM,
                    AiReviewResponse.Severity.WARNING,
                    section,
                    "Missing " + index));
        }
        Claim unsupported = claim(project, sections.get(0), "Stored Claim");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(sections);
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(unsupported));
        when(evidenceFilterService.activeMappings(unsupported)).thenReturn(List.of());
        when(claimContentConsistencyService.evaluate(unsupported))
                .thenReturn(ClaimContentStatus.PRESENT);
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(generated(
                objectMapper.writeValueAsString(Map.of(
                        "summary", "Prioritized findings",
                        "findings", generatedFindings))));

        AiReviewResponse response = service().review(document.getId(), null);

        List<AiReviewResponse.Finding> aiFindings = response.findings().stream()
                .filter(finding -> finding.type() != AiReviewResponse.FindingType.UNSUPPORTED_CLAIM)
                .toList();
        assertThat(aiFindings).hasSize(50);
        assertThat(aiFindings.stream().collect(java.util.stream.Collectors.groupingBy(
                AiReviewResponse.Finding::sectionId,
                java.util.stream.Collectors.counting())).values())
                .allMatch(count -> count <= 3);
        assertThat(aiFindings.stream()
                .filter(finding -> sections.get(0).getId().equals(finding.sectionId()))
                .map(AiReviewResponse.Finding::type))
                .containsExactlyInAnyOrder(
                        AiReviewResponse.FindingType.CLAIM_GAP,
                        AiReviewResponse.FindingType.MISSING_CLAIM,
                        AiReviewResponse.FindingType.UNNECESSARY_CLAIM);
        assertThat(response.findings())
                .hasSize(51)
                .anyMatch(finding -> finding.type()
                        == AiReviewResponse.FindingType.UNSUPPORTED_CLAIM);
    }

    @Test
    void oversizedPaperReviewsSectionsSequentiallyThenSynthesizes() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection first = section(document);
        first.setSectionTitle("First");
        first.setContentTex("a".repeat(30_000));
        PaperSection second = section(document);
        second.setSectionOrder(1);
        second.setSectionTitle("Second");
        second.setContentTex("b".repeat(30_000));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(first, second));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString()))
                .thenAnswer(generationAnswers());

        AiReviewResponse response = service().review(document.getId(), null);

        ArgumentCaptor<String> systems = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient, times(3)).generate(systems.capture(), anyString());
        assertThat(systems.getAllValues())
                .filteredOn(system -> system.contains("Review the supplied Section"))
                .hasSize(2);
        assertThat(systems.getAllValues())
                .anyMatch(system -> system.contains("Synthesize the ordered Section"));
        assertThat(response.complete()).isTrue();
        assertThat(response.coverage().sectionsScanned()).isEqualTo(2);
        assertThat(response.coverage().chunksScanned())
                .isEqualTo(response.coverage().totalChunks());
    }

    @Test
    void failedSectionDoesNotStopLaterSectionsAndPartialReviewIsNotCached() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection first = section(document);
        first.setSectionTitle("First");
        first.setContentTex("a".repeat(30_000));
        PaperSection second = section(document);
        second.setSectionOrder(1);
        second.setSectionTitle("Second");
        second.setContentTex("b".repeat(30_000));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(first, second));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(
                generated("not-json"),
                generated("still-not-json"),
                generated(validReviewJson()),
                generated(validReviewJson()));

        AiReviewResponse response = service().review(document.getId(), null);

        assertThat(response.complete()).isFalse();
        assertThat(response.direction()).isEqualTo(AiReviewResponse.Direction.INSUFFICIENT_DATA);
        assertThat(response.coverage().sectionsScanned()).isEqualTo(1);
        assertThat(response.limitations()).anyMatch(value -> value.contains("First"));
        verify(aiModelClient, times(4)).generate(anyString(), anyString());
        verify(reviewSnapshotRepository, never()).save(any());
    }

    @Test
    void archivedProjectRejectsSectionMutation() {
        User user = instructor();
        Project project = project();
        project.setStatus(ProjectStatus.ARCHIVED);
        Document document = document(project);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Project is read-only."))
                .when(currentUserService).requireProjectWriteAccess(user, project);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().createSection(
                document.getId(), "Conclusion", null))
                .hasMessageContaining("Project is read-only.");
    }

    @Test
    void updateSectionRejectsSectionFromAnotherDocument() {
        User user = instructor();
        Document authorized = document(project());
        PaperSection foreign = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(authorized.getId()))
                .thenReturn(List.of());
        when(paperSectionRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().updateSection(
                authorized.getId(), foreign.getId(), "Changed", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void mergeRejectsTargetFromAnotherDocument() {
        User user = instructor();
        Document authorized = document(project());
        PaperSection source = section(authorized);
        PaperSection foreignTarget = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(authorized.getId()))
                .thenReturn(List.of());
        when(paperSectionRepository.findById(foreignTarget.getId())).thenReturn(Optional.of(foreignTarget));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().updateSection(
                authorized.getId(), source.getId(), null, null, foreignTarget.getId(), null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void contentUpdateUsesSectionContentPermission() {
        User user = user();
        Document document = document(project());
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().updateSection(document.getId(), section.getId(), null, null, null, "Updated");

        verify(currentUserService).requireSectionContentWriteAccess(user, section);
    }

    @Test
    void instructorCanRenameUnassignedSection() {
        User user = instructor();
        Document document = document(project());
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().updateSection(
                document.getId(), section.getId(), "Renamed", null, null, null);

        assertThat(section.getSectionTitle()).isEqualTo("Renamed");
        verify(currentUserService, never()).requireSectionContentWriteAccess(any(), any());
    }

    @Test
    void studentCannotRenameSection() {
        User user = user();
        Document document = document(project());
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service().updateSection(
                document.getId(), section.getId(), "Renamed", null, null, null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Only instructors");

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void structureChangesAreLockedWhenAnySectionIsAssigned() {
        User user = instructor();
        Document document = document(project());
        PaperSection assigned = section(document);
        assigned.setAssignedUser(user());
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(assigned));

        assertThatThrownBy(() -> service().updateSection(
                document.getId(), assigned.getId(), "Renamed", null, null, null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("structure is locked");

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void updateRejectsMixedStructureAndContent() {
        assertThatThrownBy(() -> service().updateSection(
                UUID.randomUUID(), UUID.randomUUID(), "Renamed", null, null, "Content"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("must be updated separately");
    }

    @Test
    void instructorCanCreateTopLevelSectionWhileStructureIsUnlocked() {
        User user = instructor();
        Document document = document(project());
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of());
        when(paperStandardService.getSectionTemplate(
                com.evidencepilot.model.enums.PaperStandard.CUSTOM, "New Section"))
                .thenReturn("% template");
        when(paperSectionRepository.save(any(PaperSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().createSection(document.getId(), "New Section", null);

        verify(paperSectionRepository).save(argThat(section ->
                section.getDocument() == document
                        && section.getAssignedUser() == null
                        && section.getSectionOrder() == 0
                        && "New Section".equals(section.getSectionTitle())
                        && "% template".equals(section.getContentTex())));
    }

    @Test
    void mergeMovesActiveClaimsToTargetSection() {
        User user = instructor();
        Document document = document(project());
        PaperSection source = section(document);
        PaperSection target = section(document);
        Claim claim = new Claim();
        claim.setActive(true);
        claim.setSection(source);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(source, target));
        when(paperSectionRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(paperSectionRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(claimRepository.findBySectionId(source.getId())).thenReturn(List.of(claim));

        service().updateSection(document.getId(), source.getId(), null, null, target.getId(), null);

        assertThat(claim.getSection()).isSameAs(target);
        verify(claimRepository).saveAll(List.of(claim));
    }

    @Test
    void deleteSectionRejectsActiveClaims() {
        User user = instructor();
        Document document = document(project());
        PaperSection section = section(document);
        section.setContentTex("");
        Claim claim = new Claim();
        claim.setActive(true);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(claimRepository.findBySectionId(section.getId())).thenReturn(List.of(claim));

        assertThatThrownBy(() -> service().deleteSection(document.getId(), section.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("active claims");
        verify(paperSectionRepository, never()).save(section);
    }

    @Test
    void deleteSectionRejectsInstructorFeedback() {
        User user = instructor();
        Document document = document(project());
        PaperSection section = section(document);
        section.setContentTex("");
        InstructorFeedback feedback = new InstructorFeedback();
        feedback.setSection(section);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(document.getProject().getId()))
                .thenReturn(List.of(feedback));

        assertThatThrownBy(() -> service().deleteSection(document.getId(), section.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("feedback");

        verify(paperSectionRepository, never()).save(section);
    }

    @Test
    void resetStandardRejectsSectionsWithActiveClaims() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        section.setContentTex("");
        Claim claim = new Claim();
        claim.setActive(true);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                project.getId(), DocumentType.PAPER)).thenReturn(List.of(document));
        when(documentRepository.save(document)).thenReturn(document);
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(claimRepository.findBySectionId(section.getId())).thenReturn(List.of(claim));

        assertThatThrownBy(() -> service().resetSectionsForStandard(project.getId(), "IEEE"))
                .hasMessageContaining("active claims");
        verify(paperSectionRepository, never()).deleteAll(any());
    }

    @Test
    void texExportRemainsAvailableWhenClaimsAreInconsistent() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.APPROVED);
        Path archive = Path.of("export.zip");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(texArchiveBuilder.build(project.getId())).thenReturn(archive);

        assertThat(service().exportTexArchive(project.getId())).isEqualTo(archive);
        verify(texArchiveBuilder).build(project.getId());
    }

    @Test
    void createSectionRejectsParentFromAnotherDocument() {
        User user = instructor();
        Document authorized = document(project());
        PaperSection foreignParent = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(authorized.getId()))
                .thenReturn(List.of());
        when(paperSectionRepository.findById(foreignParent.getId())).thenReturn(Optional.of(foreignParent));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().createSection(
                authorized.getId(), "Conclusion", foreignParent.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void detectAndPersistSectionsReturnsEmptyWithoutExtractedText() {
        Document document = document(project());
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        org.assertj.core.api.Assertions.assertThat(service().detectAndPersistSections(document.getId())).isEmpty();
    }

    @Test
    void detectAndPersistSectionsParsesHeadingsIntoOrderedSections() {
        Document document = document(project());
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("# Introduction\nopening content\n\n# Methodology\nmethod content");
        document.setDocumentText(text);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> {
                    List<PaperSection> saved = new java.util.ArrayList<>(invocation.getArgument(0));
                    saved.forEach(section -> section.setId(UUID.randomUUID()));
                    return saved;
                });

        service().detectAndPersistSections(document.getId());

        ArgumentCaptor<Iterable<PaperSection>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(paperSectionRepository).saveAll(captor.capture());
        var saved = java.util.stream.StreamSupport
                .stream(captor.getValue().spliterator(), false)
                .toList();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getSectionTitle()).isEqualTo("Introduction");
        assertThat(saved.get(0).getSectionOrder()).isEqualTo(0);
        assertThat(saved.get(0).getContentTex()).isEqualTo("opening content");
        assertThat(saved.get(1).getSectionTitle()).isEqualTo("Methodology");
        assertThat(saved.get(1).getSectionOrder()).isEqualTo(1);
        assertThat(saved.get(1).getContentTex()).isEqualTo("method content");
    }

    @Test
    void detectAndPersistSectionsCreatesFullTextSection() {
        Document document = document(project());
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("lowercase content without a heading");
        document.setDocumentText(text);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().detectAndPersistSections(document.getId());

        verify(paperSectionRepository).saveAll(argThat(sections -> {
            var iterator = sections.iterator();
            return iterator.hasNext()
                    && iterator.next().getSectionTitle().equals("Full Text")
                    && !iterator.hasNext();
        }));
    }

    @Test
    void detectAndPersistSectionsKeepsExistingSectionsOnRetry() {
        Document document = document(project());
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("Introduction\nExtracted content");
        document.setDocumentText(text);
        PaperSection existing = new PaperSection();
        existing.setId(UUID.randomUUID());
        existing.setDocument(document);
        existing.setSectionTitle("Edited Introduction");
        existing.setSectionOrder(0);

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(existing));

        org.assertj.core.api.Assertions.assertThat(service().detectAndPersistSections(document.getId()))
                .hasSize(1);

        verify(paperSectionRepository).findByDocumentIdOrderBySectionOrderAsc(document.getId());
        verifyNoMoreInteractions(paperSectionRepository);
    }

    private PaperProcessingServiceImpl service() {
        return new PaperProcessingServiceImpl(
                aiModelClient,
                paperSectionRepository,
                claimRepository,
                claimEvidenceMappingRepository,
                instructorFeedbackRepository,
                claimContentConsistencyService,
                evidenceFilterService,
                auditService,
                objectMapper,
                documentRepository,
                projectMapper,
                currentUserService,
                paperStandardService,
                userRepository,
                projectRepository,
                systemNotificationService,
                texArchiveBuilder,
                reviewSnapshotRepository);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(user.getId() + "@example.com");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private User instructor() {
        User user = user();
        user.setRole(UserRole.INSTRUCTOR);
        return user;
    }

    private Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Capstone");
        project.setActive(true);
        return project;
    }

    private Document document(Project project) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);
        return document;
    }

    private PaperSection section(Document document) {
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        section.setSectionTitle("Section");
        section.setSectionOrder(0);
        section.setContentTex("Content");
        return section;
    }

    private Claim claim(Project project, PaperSection section, String content) {
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent(content);
        claim.setClaimVersion(1);
        claim.setActive(true);
        return claim;
    }

    private AiReviewResponse.Finding finding(
            AiReviewResponse.FindingType type,
            AiReviewResponse.Severity severity,
            PaperSection section,
            String message) {
        return new AiReviewResponse.Finding(
                type,
                severity,
                null,
                section.getId(),
                List.of(),
                List.of(),
                "",
                message,
                "Fix this issue.");
    }

    private ClaimEvidenceMapping mapping(String snippet, MappingStatus status) {
        Document source = new Document();
        source.setId(UUID.randomUUID());
        source.setOriginalFilename("source.pdf");
        source.setDocType(DocumentType.SOURCE);
        source.setActive(true);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(source);
        chunk.setText(snippet);
        chunk.setActive(true);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setDocumentChunk(chunk);
        mapping.setStrengthScore(80);
        mapping.setStatus(status);
        return mapping;
    }

    private String validReviewJson() {
        return """
                {
                  "summary":"No priority Claim issues found.",
                  "findings":[]
                }
                """;
    }

    private org.mockito.stubbing.Answer<AiModelClient.GenerationResult> generationAnswers() {
        return invocation -> new AiModelClient.GenerationResult(
                "ollama", "qwen3.5:9b", validReviewJson());
    }

    private AiModelClient.GenerationResult generated(String response) {
        return new AiModelClient.GenerationResult("ollama", "qwen3.5:9b", response);
    }
}
