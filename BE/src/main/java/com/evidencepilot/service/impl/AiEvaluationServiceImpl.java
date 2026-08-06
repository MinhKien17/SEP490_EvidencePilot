package com.evidencepilot.service.impl;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.dto.response.JobResponse;
import com.evidencepilot.dto.response.JobSubmitResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.AiEvaluationJob;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.repository.AiEvaluationJobRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.AiEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationServiceImpl implements AiEvaluationService {

    private final AiEvaluationJobRepository jobRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final SectionCitationReviewService sectionCitationReviewService;
    private final SectionAuditService sectionAuditService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public JobSubmitResponse submit(UUID projectId, String kind, String payloadJson) {
        AiEvaluationJob job = new AiEvaluationJob();
        job.setProjectId(projectId);
        job.setKind(kind);
        job.setPayloadJson(payloadJson);
        job.setStatus(AiEvaluationJob.STATUS_PENDING);
        job.setCreatedAt(LocalDateTime.now());
        jobRepository.save(job);
        publish(job);
        return new JobSubmitResponse(job.getId());
    }

    @Override
    public synchronized JobSubmitResponse submitSectionCitationReview(
            UUID projectId,
            UUID documentId,
            UUID sectionId,
            String contentFingerprint,
            UUID requestedByUserId) {
        for (AiEvaluationJob job : jobRepository
                .findByProjectIdAndKindAndStatusInOrderByCreatedAtDesc(
                        projectId,
                        AiEvaluationJob.KIND_SECTION_CITATION_REVIEW,
                        List.of(AiEvaluationJob.STATUS_PENDING, AiEvaluationJob.STATUS_PROCESSING))) {
            if (sameSectionCitationReview(job, documentId, sectionId, contentFingerprint)) {
                return new JobSubmitResponse(job.getId());
            }
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "documentId", documentId,
                    "projectId", projectId,
                    "sectionId", sectionId,
                    "contentFingerprint", contentFingerprint,
                    "requestedByUserId", requestedByUserId));
            return submit(projectId, AiEvaluationJob.KIND_SECTION_CITATION_REVIEW, payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize section citation review job", exception);
        }
    }

    @Override
    public synchronized JobSubmitResponse submitSectionAudit(
            UUID projectId,
            UUID sectionId,
            String contentFingerprint,
            UUID requestedByUserId) {
        for (AiEvaluationJob job : jobRepository
                .findByProjectIdAndKindAndStatusInOrderByCreatedAtDesc(
                        projectId,
                        AiEvaluationJob.KIND_SECTION_AUDIT,
                        List.of(AiEvaluationJob.STATUS_PENDING, AiEvaluationJob.STATUS_PROCESSING))) {
            if (sameSectionAudit(job, sectionId, contentFingerprint)) {
                return new JobSubmitResponse(job.getId());
            }
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "projectId", projectId,
                    "sectionId", sectionId,
                    "contentFingerprint", contentFingerprint,
                    "requestedByUserId", requestedByUserId));
            return submit(projectId, AiEvaluationJob.KIND_SECTION_AUDIT, payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize section audit job", exception);
        }
    }

    @Override
    public void process(UUID jobId) {
        AiEvaluationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("AI evaluation job {} not found, skipping", jobId);
            return;
        }
        if (!AiEvaluationJob.STATUS_PENDING.equals(job.getStatus())) {
            return;
        }
        job.setStatus(AiEvaluationJob.STATUS_PROCESSING);
        jobRepository.save(job);
        try {
            job.setResultJson(objectMapper.writeValueAsString(run(job)));
            job.setStatus(AiEvaluationJob.STATUS_SUCCESS);
        } catch (Exception e) {
            log.warn("AI evaluation job {} ({}) failed: {}", job.getId(), job.getKind(), e.getMessage());
            job.setErrorMessage(e.getMessage());
            job.setStatus(AiEvaluationJob.STATUS_FAILED);
        }
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    public JobResponse getJob(UUID jobId) {
        AiEvaluationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(jobId, "AiEvaluationJob"));
        JsonNode result = null;
        if (job.getResultJson() != null) {
            try {
                result = objectMapper.readTree(job.getResultJson());
            } catch (Exception e) {
                log.warn("Job {} result is not valid JSON", jobId);
            }
        }
        return new JobResponse(
                job.getId(), job.getProjectId(), job.getKind(), job.getStatus(),
                result, job.getErrorMessage(), job.getCompletedAt());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reenqueuePendingJobs() {
        List<AiEvaluationJob> pending = jobRepository.findByStatus(AiEvaluationJob.STATUS_PENDING);
        for (AiEvaluationJob job : pending) {
            publish(job);
        }
        if (!pending.isEmpty()) {
            log.info("Re-enqueued {} pending AI evaluation jobs", pending.size());
        }
    }

    private JsonNode run(AiEvaluationJob job) throws Exception {
        JsonNode payload = objectMapper.readTree(job.getPayloadJson());
        return switch (job.getKind()) {
            case AiEvaluationJob.KIND_SECTION_CITATION_REVIEW -> {
                UUID documentId = UUID.fromString(payload.path("documentId").asText());
                UUID projectId = UUID.fromString(payload.path("projectId").asText());
                UUID sectionId = UUID.fromString(payload.path("sectionId").asText());
                UUID requestedByUserId = UUID.fromString(
                        payload.path("requestedByUserId").asText());
                if (!job.getProjectId().equals(projectId)) {
                    throw new IllegalArgumentException(
                            "Section review payload project does not match its job");
                }
                yield objectMapper.valueToTree(sectionCitationReviewService.run(
                        documentId,
                        projectId,
                        sectionId,
                        payload.path("contentFingerprint").asText(),
                        requestedByUserId));
            }
            case AiEvaluationJob.KIND_SECTION_AUDIT -> {
                UUID projectId = UUID.fromString(payload.path("projectId").asText());
                UUID sectionId = UUID.fromString(payload.path("sectionId").asText());
                UUID requestedByUserId = UUID.fromString(
                        payload.path("requestedByUserId").asText());
                if (!job.getProjectId().equals(projectId)) {
                    throw new IllegalArgumentException(
                            "Section audit payload project does not match its job");
                }
                yield objectMapper.valueToTree(sectionAuditService.run(
                        projectId,
                        sectionId,
                        payload.path("contentFingerprint").asText(),
                        requestedByUserId));
            }
            default -> throw new IllegalStateException("Unknown AI evaluation job kind: " + job.getKind());
        };
    }

    private boolean sameSectionAudit(AiEvaluationJob job, UUID sectionId, String contentFingerprint) {
        try {
            JsonNode payload = objectMapper.readTree(job.getPayloadJson());
            return sectionId.toString().equals(payload.path("sectionId").asText())
                    && contentFingerprint.equals(payload.path("contentFingerprint").asText());
        } catch (Exception exception) {
            log.warn("Section audit job {} has an invalid payload; not reusing it", job.getId());
            return false;
        }
    }

    private boolean sameSectionCitationReview(
            AiEvaluationJob job,
            UUID documentId,
            UUID sectionId,
            String contentFingerprint) {
        try {
            JsonNode payload = objectMapper.readTree(job.getPayloadJson());
            return documentId.toString().equals(payload.path("documentId").asText())
                    && sectionId.toString().equals(payload.path("sectionId").asText())
                    && contentFingerprint.equals(payload.path("contentFingerprint").asText());
        } catch (Exception exception) {
            log.warn("Section review job {} has an invalid payload; not reusing it", job.getId());
            return false;
        }
    }

    private void publish(AiEvaluationJob job) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AI_EVALUATION_QUEUE, Map.of("jobId", job.getId().toString()));
        } catch (Exception e) {
            // job stays PENDING and is re-enqueued on next startup
            log.error("Failed to publish AI evaluation job {}: {}", job.getId(), e.getMessage());
        }
    }
}
