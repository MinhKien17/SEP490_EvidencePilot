package com.evidencepilot.service.impl;

import com.evidencepilot.model.AiEvaluationJob;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.repository.AiEvaluationJobRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.AiEvaluationService;
import com.evidencepilot.service.ClaimMatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiEvaluationService service() {
        return new AiEvaluationServiceImpl(
                jobRepository, paperSectionRepository, qualityService, matchingService,
                rabbitTemplate, objectMapper);
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
        when(paperSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
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
    void reenqueuePendingJobs_publishesEachPendingJob() {
        UUID jobId = UUID.randomUUID();
        AiEvaluationJob pending = job(UUID.randomUUID(), UUID.randomUUID(), "{\"x\":1}");
        pending.setId(jobId);
        when(jobRepository.findByStatus(AiEvaluationJob.STATUS_PENDING)).thenReturn(List.of(pending));

        new AiEvaluationServiceImpl(
                jobRepository, paperSectionRepository, qualityService, matchingService,
                rabbitTemplate, objectMapper).reenqueuePendingJobs();

        verify(rabbitTemplate).convertAndSend(
                com.evidencepilot.config.infrastructure.RabbitMQConfig.AI_EVALUATION_QUEUE,
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
