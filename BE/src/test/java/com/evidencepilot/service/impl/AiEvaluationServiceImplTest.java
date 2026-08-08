package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.model.AiEvaluationJob;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ReviewGuide;
import com.evidencepilot.repository.AiEvaluationJobRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ReviewGuideRepository;
import com.evidencepilot.service.AiModelClient;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiEvaluationServiceImplTest {

    private final AiEvaluationJobRepository jobRepository = mock(AiEvaluationJobRepository.class);
    private final PaperSectionRepository paperSectionRepository = mock(PaperSectionRepository.class);
    private final SectionCitationReviewService sectionCitationReviewService = mock(SectionCitationReviewService.class);
    private final ReviewGuideRepository reviewGuideRepository = mock(ReviewGuideRepository.class);
    private final AiModelClient aiModelClient = mock(AiModelClient.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private AiEvaluationServiceImpl service() {
        return new AiEvaluationServiceImpl(
                jobRepository, paperSectionRepository, sectionCitationReviewService,
                reviewGuideRepository, aiModelClient,
                rabbitTemplate, objectMapper);
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

        var response = service().submit(projectId, AiEvaluationJob.KIND_SECTION_CITATION_REVIEW, "{\"x\":1}");

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
    void process_sectionSuggestion_extractsArrayAndStoresSuccess() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        AiEvaluationJob job = job(sectionId, projectId, objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "documentId", documentId,
                "sectionId", sectionId,
                "sectionType", "Introduction")));
        job.setKind(AiEvaluationJob.KIND_SECTION_SUGGESTION);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        PaperSection section = new PaperSection();
        section.setId(sectionId);
        section.setSectionTitle("Introduction");
        section.setContentTex("The research question is stated.");
        Document document = new Document();
        document.setId(documentId);
        Project project = new Project();
        project.setId(projectId);
        document.setProject(project);
        section.setDocument(document);
        when(paperSectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        ReviewGuide guide = new ReviewGuide();
        guide.setSectionType("Introduction");
        guide.setChecklistJson("[\"Is the research question stated?\"]");
        when(reviewGuideRepository.findById("Introduction")).thenReturn(Optional.of(guide));
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(
                new AiModelClient.GenerationResult("provider", "model",
                        "```json\n[{\"issue\":\"Gap\",\"quote\":\"research question is stated\",\"actionable_fix\":\"Restate it clearly.\"}]\n```\n trailing"));

        service().process(jobId);

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_SUCCESS);
        assertThat(job.getResultJson()).contains("actionable_fix");
        verify(aiModelClient).generate(anyString(), anyString());
        verify(reviewGuideRepository).findById("Introduction");
    }

    @Test
    void process_sectionSuggestion_refusalText_marksFailed() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        AiEvaluationJob job = job(sectionId, projectId, objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "documentId", documentId,
                "sectionId", sectionId,
                "sectionType", "Introduction")));
        job.setKind(AiEvaluationJob.KIND_SECTION_SUGGESTION);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        PaperSection section = new PaperSection();
        section.setId(sectionId);
        section.setSectionTitle("Introduction");
        section.setContentTex("Some content");
        Document document = new Document();
        document.setId(documentId);
        Project project = new Project();
        project.setId(projectId);
        document.setProject(project);
        section.setDocument(document);
        when(paperSectionRepository.findByIdWithDocument(sectionId)).thenReturn(Optional.of(section));
        ReviewGuide guide = new ReviewGuide();
        guide.setSectionType("Introduction");
        guide.setChecklistJson("[\"Is the research question stated?\"]");
        when(reviewGuideRepository.findById("Introduction")).thenReturn(Optional.of(guide));
        when(aiModelClient.generate(anyString(), anyString())).thenReturn(
                new AiModelClient.GenerationResult("provider", "model", "I cannot fulfill this request."));

        service().process(jobId);

        assertThat(job.getStatus()).isEqualTo(AiEvaluationJob.STATUS_FAILED);
        assertThat(job.getErrorMessage()).isNotBlank();
        assertThat(job.getCompletedAt()).isNotNull();
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
