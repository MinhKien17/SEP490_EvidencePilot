package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.SectionAuditResponse;
import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.model.AiEvaluationJob;
import com.evidencepilot.repository.AiEvaluationJobRepository;
import com.evidencepilot.repository.PaperSectionRepository;
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
    private final SectionCitationReviewService sectionCitationReviewService = mock(SectionCitationReviewService.class);
    private final SectionAuditService sectionAuditService = mock(SectionAuditService.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private AiEvaluationServiceImpl service() {
        return new AiEvaluationServiceImpl(
                jobRepository, paperSectionRepository, sectionCitationReviewService,
                sectionAuditService, rabbitTemplate, objectMapper);
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

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_SUCCESS);
    }

    @Test
    void submit_persistsPendingJobAndPublishes() {
        UUID projectId = UUID.randomUUID();
        when(jobRepository.save(any(AiEvaluationJob.class))).thenAnswer(invocation -> {
            AiEvaluationJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        var response = service().submit(projectId, AiEvaluationJob.KIND_SECTION_AUDIT, "{\"x\":1}");

        assertThat(response.jobId()).isNotNull();
        verify(rabbitTemplate).convertAndSend(
                eq(com.evidencepilot.config.infrastructure.RabbitMQConfig.AI_EVALUATION_QUEUE),
                any(Map.class));
    }

    @Test
    void submitSectionCitationReview_publishes() {
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
                eq(com.evidencepilot.config.infrastructure.RabbitMQConfig.AI_EVALUATION_QUEUE),
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
    void process_sectionAudit_runsSectionAudit() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        AiEvaluationJob job = job(
                sectionId,
                projectId,
                objectMapper.writeValueAsString(Map.of(
                        "projectId", projectId,
                        "sectionId", sectionId,
                        "contentFingerprint", "fingerprint",
                        "requestedByUserId", requesterId)));
        job.setKind(AiEvaluationJob.KIND_SECTION_AUDIT);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(sectionAuditService.run(projectId, sectionId, "fingerprint", requesterId))
                .thenReturn(new SectionAuditResponse(sectionId, "fingerprint", List.of()));

        service().process(jobId);

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_SUCCESS);
        assertThat(job.getResultJson()).contains("\"fingerprint\"");
        verify(sectionAuditService).run(projectId, sectionId, "fingerprint", requesterId);
    }

    @Test
    void submitSectionAudit_dedupesInFlightJob() {
        UUID projectId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        AiEvaluationJob inFlight = job(sectionId, projectId, "{\"sectionId\":\""
                + sectionId + "\",\"contentFingerprint\":\"fp\",\"projectId\":\""
                + projectId + "\",\"requestedByUserId\":\""
                + UUID.randomUUID() + "\"}");
        inFlight.setKind(AiEvaluationJob.KIND_SECTION_AUDIT);
        when(jobRepository.findByProjectIdAndKindAndStatusInOrderByCreatedAtDesc(
                eq(projectId),
                eq(AiEvaluationJob.KIND_SECTION_AUDIT),
                any())).thenReturn(List.of(inFlight));

        var response = service().submitSectionAudit(projectId, sectionId, "fp", UUID.randomUUID());

        assertThat(response.jobId()).isEqualTo(inFlight.getId());
        verify(jobRepository, org.mockito.Mockito.never()).save(any(AiEvaluationJob.class));
    }

    @Test
    void reenqueuePendingJobs_publishesEachPendingJob() {
        UUID jobId = UUID.randomUUID();
        AiEvaluationJob pending = job(UUID.randomUUID(), UUID.randomUUID(), "{\"x\":1}");
        pending.setId(jobId);
        when(jobRepository.findByStatus(AiEvaluationJob.STATUS_PENDING)).thenReturn(List.of(pending));

        service().reenqueuePendingJobs();

        verify(rabbitTemplate).convertAndSend(
                com.evidencepilot.config.infrastructure.RabbitMQConfig.AI_EVALUATION_QUEUE,
                Map.of("jobId", jobId.toString()));
    }

    private AiEvaluationJob job(UUID sectionId, UUID projectId, String payloadJson) {
        AiEvaluationJob job = new AiEvaluationJob();
        job.setId(UUID.randomUUID());
        job.setProjectId(projectId);
        job.setKind(AiEvaluationJob.KIND_SECTION_CITATION_REVIEW);
        job.setPayloadJson(payloadJson);
        job.setStatus(AiEvaluationJob.STATUS_PENDING);
        return job;
    }
}
