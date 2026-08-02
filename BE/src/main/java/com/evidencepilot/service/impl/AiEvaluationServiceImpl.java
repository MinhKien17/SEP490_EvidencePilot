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
import com.evidencepilot.service.ClaimMatchingService;
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
    private final ClaimQualityEvaluationService claimQualityEvaluationService;
    private final ClaimMatchingService claimMatchingService;
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
        publish(job.getId());
        return new JobSubmitResponse(job.getId());
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
            publish(job.getId());
        }
        if (!pending.isEmpty()) {
            log.info("Re-enqueued {} pending AI evaluation jobs", pending.size());
        }
    }

    private JsonNode run(AiEvaluationJob job) throws Exception {
        JsonNode payload = objectMapper.readTree(job.getPayloadJson());
        return switch (job.getKind()) {
            case AiEvaluationJob.KIND_CLAIM_QUALITY -> {
                UUID sectionId = UUID.fromString(payload.path("sectionId").asText());
                String content = payload.path("content").asText();
                PaperSection section = paperSectionRepository.findById(sectionId)
                        .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
                yield objectMapper.valueToTree(claimQualityEvaluationService.evaluate(
                        section.getDocument().getProject(), section, content));
            }
            case AiEvaluationJob.KIND_MATCH_EVALUATION -> {
                UUID claimId = UUID.fromString(payload.path("claimId").asText());
                UUID projectId = UUID.fromString(payload.path("projectId").asText());
                UUID documentChunkId = UUID.fromString(payload.path("documentChunkId").asText());
                yield objectMapper.valueToTree(claimMatchingService.evaluateMatch(
                        claimId, projectId, documentChunkId));
            }
            default -> throw new IllegalStateException("Unknown AI evaluation job kind: " + job.getKind());
        };
    }

    private void publish(UUID jobId) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AI_EVALUATION_QUEUE, Map.of("jobId", jobId.toString()));
        } catch (Exception e) {
            // job stays PENDING and is re-enqueued on next startup
            log.error("Failed to publish AI evaluation job {}: {}", jobId, e.getMessage());
        }
    }
}
