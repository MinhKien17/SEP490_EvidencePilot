package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.AiReviewResponse;
import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.model.AiEvaluationJob;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.repository.AiEvaluationJobRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.ClaimMatchingService;
import com.evidencepilot.service.PaperProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiEvaluationServiceImplTest {

    private final AiEvaluationJobRepository jobRepository = mock(AiEvaluationJobRepository.class);
    private final PaperSectionRepository paperSectionRepository = mock(PaperSectionRepository.class);
    private final ClaimQualityEvaluationService qualityService = mock(ClaimQualityEvaluationService.class);
    private final ClaimMatchingService matchingService = mock(ClaimMatchingService.class);
    private final PaperProcessingService paperProcessingService = mock(PaperProcessingService.class);
    private final SectionCitationReviewService sectionCitationReviewService = mock(SectionCitationReviewService.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private AiEvaluationServiceImpl service() {
        return new AiEvaluationServiceImpl(
                jobRepository, paperSectionRepository, qualityService, matchingService,
                paperProcessingService, sectionCitationReviewService, rabbitTemplate, objectMapper);
    }

    @Test
    void process_claimQuality_marksSuccessWithResult() {
        UUID jobId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        Document document = new Document();
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setId(sectionId);
        section.setDocument(document);
        AiEvaluationJob job = job(sectionId, projectId, "{\"sectionId\":\"" + sectionId + "\",\"content\":\"draft\"}");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(paperSectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        when(qualityService.evaluate(project, section, "draft")).thenReturn(
                com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.from(
                        List.of(
                                new com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.Criterion(
                                        com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.CriterionCode.CLARITY, 2, "clear"),
                                new com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.Criterion(
                                        com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.CriterionCode.SPECIFICITY_SCOPE, 2, "specific"),
                                new com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.Criterion(
                                        com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.CriterionCode.SECTION_RELEVANCE, 2, "relevant"),
                                new com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.Criterion(
                                        com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.CriterionCode.VERIFIABILITY_ARGUABILITY, 2, "verifiable"),
                                new com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.Criterion(
                                        com.evidencepilot.dto.response.ClaimQualityEvaluationResponse.CriterionCode.ATOMICITY, 2, "atomic")),
                        com.evidencepilot.model.enums.FunctionalType.EMPIRICAL,
                        "revision"));

        service().process(jobId);

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_SUCCESS);
        assertThat(job.getResultJson()).contains("\"score\"");
        assertThat(job.getErrorMessage()).isNull();
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void process_invalidPayload_marksFailedWithError() {
        UUID jobId = UUID.randomUUID();
        AiEvaluationJob job = job(UUID.randomUUID(), UUID.randomUUID(), "not-json");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service().process(jobId);

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_FAILED);
        assertThat(job.getErrorMessage()).isNotBlank();
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void process_alreadyProcessedJob_isSkipped() {
        UUID jobId = UUID.randomUUID();
        AiEvaluationJob job = job(UUID.randomUUID(), UUID.randomUUID(), "{\"x\":1}");
        job.setStatus(AiEvaluationJob.STATUS_SUCCESS);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service().process(jobId);

        verify(qualityService, org.mockito.Mockito.never()).evaluate(any(), any(), any());
    }

    @Test
    void submit_persistsPendingJobAndPublishes() {
        UUID projectId = UUID.randomUUID();
        when(jobRepository.save(any(AiEvaluationJob.class))).thenAnswer(invocation -> {
            AiEvaluationJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        var response = service().submit(projectId, AiEvaluationJob.KIND_CLAIM_QUALITY, "{\"x\":1}");

        assertThat(response.jobId()).isNotNull();
        verify(rabbitTemplate).convertAndSend(
                eq(com.evidencepilot.config.infrastructure.RabbitMQConfig.AI_EVALUATION_QUEUE),
                any(Map.class));
    }

    @Test
    void submitPaperReview_reusesMatchingActiveJob() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        AiEvaluationJob existing = job(
                UUID.randomUUID(),
                projectId,
                objectMapper.writeValueAsString(Map.of(
                        "documentId", documentId,
                        "projectId", projectId,
                        "targetStyle", "APA",
                        "requestedByUserId", requesterId)));
        existing.setKind(AiEvaluationJob.KIND_PAPER_REVIEW);
        when(jobRepository.findByProjectIdAndKindAndStatusInOrderByCreatedAtDesc(
                eq(projectId),
                eq(AiEvaluationJob.KIND_PAPER_REVIEW),
                any())).thenReturn(List.of(existing));

        var response = service().submitPaperReview(
                projectId, documentId, " APA ", requesterId);

        assertThat(response.jobId()).isEqualTo(existing.getId());
        verify(jobRepository, org.mockito.Mockito.never()).save(any());
        verify(rabbitTemplate, org.mockito.Mockito.never())
                .convertAndSend(any(String.class), any(Map.class));
    }

    @Test
    void submitPaperReview_publishesToDedicatedQueue() {
        UUID projectId = UUID.randomUUID();
        when(jobRepository.findByProjectIdAndKindAndStatusInOrderByCreatedAtDesc(
                eq(projectId),
                eq(AiEvaluationJob.KIND_PAPER_REVIEW),
                any())).thenReturn(List.of());
        when(jobRepository.save(any(AiEvaluationJob.class))).thenAnswer(invocation -> {
            AiEvaluationJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        var response = service().submitPaperReview(
                projectId, UUID.randomUUID(), null, UUID.randomUUID());

        assertThat(response.jobId()).isNotNull();
        verify(rabbitTemplate).convertAndSend(
                eq(com.evidencepilot.config.infrastructure.RabbitMQConfig.PAPER_REVIEW_QUEUE),
                any(Map.class));
    }

    @Test
    void process_paperReview_runsBackgroundSafeReview() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        AiEvaluationJob job = job(
                UUID.randomUUID(),
                projectId,
                objectMapper.writeValueAsString(Map.of(
                        "documentId", documentId,
                        "projectId", projectId,
                        "targetStyle", "default",
                        "requestedByUserId", requesterId)));
        job.setKind(AiEvaluationJob.KIND_PAPER_REVIEW);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(paperProcessingService.runReview(
                documentId, projectId, "default", requesterId)).thenReturn(
                        new AiReviewResponse(
                                "paper-claim-review-v7",
                                true,
                                new AiReviewResponse.Coverage(1, 1, 1, 1, 0, 0),
                                AiReviewResponse.Direction.ON_TRACK,
                                "Done",
                                List.of(),
                                List.of()));

        service().process(jobId);

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_SUCCESS);
        assertThat(job.getResultJson()).contains("paper-claim-review-v7");
        verify(paperProcessingService).runReview(
                documentId, projectId, "default", requesterId);
    }

    @Test
    void submitSectionCitationReview_publishesToDedicatedQueue() {
        UUID projectId = UUID.randomUUID();
        when(jobRepository.findByProjectIdAndKindAndStatusInOrderByCreatedAtDesc(
                eq(projectId),
                eq(AiEvaluationJob.KIND_SECTION_CITATION_REVIEW),
                any())).thenReturn(List.of());
        when(jobRepository.save(any(AiEvaluationJob.class))).thenAnswer(invocation -> {
            AiEvaluationJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        var response = service().submitSectionCitationReview(
                projectId, UUID.randomUUID(), UUID.randomUUID(), "fingerprint", UUID.randomUUID());

        assertThat(response.jobId()).isNotNull();
        verify(rabbitTemplate).convertAndSend(
                eq(com.evidencepilot.config.infrastructure.RabbitMQConfig.PAPER_REVIEW_QUEUE),
                any(Map.class));
    }

    @Test
    void process_sectionCitationReview_runsSectionReview() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        AiEvaluationJob job = job(
                sectionId,
                projectId,
                objectMapper.writeValueAsString(Map.of(
                        "documentId", documentId,
                        "projectId", projectId,
                        "sectionId", sectionId,
                        "contentFingerprint", "fingerprint",
                        "requestedByUserId", requesterId)));
        job.setKind(AiEvaluationJob.KIND_SECTION_CITATION_REVIEW);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(sectionCitationReviewService.run(
                documentId, projectId, sectionId, "fingerprint", requesterId))
                .thenReturn(new SectionCitationReviewResponse(
                        "section-citation-v1",
                        "citation-rules-v1",
                        sectionId,
                        1,
                        "fingerprint",
                        LocalDateTime.now(),
                        "provider",
                        "model",
                        true,
                        "Done",
                        List.of(),
                        List.of()));

        service().process(jobId);

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_SUCCESS);
        assertThat(job.getResultJson()).contains("section-citation-v1");
        verify(sectionCitationReviewService).run(
                documentId, projectId, sectionId, "fingerprint", requesterId);
    }

    @Test
    void reenqueuePendingJobs_publishesEachPendingJob() {
        UUID jobId = UUID.randomUUID();
        AiEvaluationJob pending = job(UUID.randomUUID(), UUID.randomUUID(), "{\"x\":1}");
        pending.setId(jobId);
        when(jobRepository.findByStatus(AiEvaluationJob.STATUS_PENDING)).thenReturn(List.of(pending));

        new AiEvaluationServiceImpl(
                jobRepository, paperSectionRepository, qualityService, matchingService,
                paperProcessingService, sectionCitationReviewService, rabbitTemplate, objectMapper).reenqueuePendingJobs();

        verify(rabbitTemplate).convertAndSend(
                com.evidencepilot.config.infrastructure.RabbitMQConfig.AI_EVALUATION_QUEUE,
                Map.of("jobId", jobId.toString()));
    }

    @Test
    void reenqueuePendingPaperReview_usesDedicatedQueue() {
        UUID jobId = UUID.randomUUID();
        AiEvaluationJob pending = job(UUID.randomUUID(), UUID.randomUUID(), "{\"x\":1}");
        pending.setId(jobId);
        pending.setKind(AiEvaluationJob.KIND_PAPER_REVIEW);
        when(jobRepository.findByStatus(AiEvaluationJob.STATUS_PENDING))
                .thenReturn(List.of(pending));

        service().reenqueuePendingJobs();

        verify(rabbitTemplate).convertAndSend(
                com.evidencepilot.config.infrastructure.RabbitMQConfig.PAPER_REVIEW_QUEUE,
                Map.of("jobId", jobId.toString()));
    }

    private AiEvaluationJob job(UUID sectionId, UUID projectId, String payloadJson) {
        AiEvaluationJob job = new AiEvaluationJob();
        job.setId(UUID.randomUUID());
        job.setProjectId(projectId);
        job.setKind(AiEvaluationJob.KIND_CLAIM_QUALITY);
        job.setPayloadJson(payloadJson);
        job.setStatus(AiEvaluationJob.STATUS_PENDING);
        return job;
    }
}
