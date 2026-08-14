package com.evidencepilot.service.impl;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.dto.request.SectionReviewSourceMatchRequest;
import com.evidencepilot.dto.response.JobResponse;
import com.evidencepilot.dto.response.JobSubmitResponse;
import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.dto.response.SectionSuggestionDto;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.AiEvaluationJob;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.ReviewGuide;
import com.evidencepilot.prompt.SectionSuggestionPrompt;
import com.evidencepilot.repository.AiEvaluationJobRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ReviewGuideRepository;
import com.evidencepilot.service.AiEvaluationService;
import com.evidencepilot.service.AiModelClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private final ReviewGuideRepository reviewGuideRepository;
    private final AiModelClient aiModelClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final EvidenceTraceService evidenceTraceService;

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
    public JobSubmitResponse submitSectionSuggestion(
            UUID projectId,
            UUID documentId,
            UUID sectionId,
            String sectionType) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "projectId", projectId,
                    "documentId", documentId,
                    "sectionId", sectionId,
                    "sectionType", sectionType));
            return submit(projectId, AiEvaluationJob.KIND_SECTION_SUGGESTION, payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize section suggestion job", exception);
        }
    }

    @Override
    public JobSubmitResponse submitSourceMatches(
            UUID projectId,
            UUID documentId,
            UUID sectionId,
            List<SectionReviewSourceMatchRequest.Finding> findings) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "projectId", projectId,
                    "documentId", documentId,
                    "sectionId", sectionId,
                    "findings", findings));
            return submit(projectId, AiEvaluationJob.KIND_SOURCE_MATCHES, payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize source matches job", exception);
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
        job.setStartedAt(LocalDateTime.now());
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
    public void markFailed(UUID jobId, String error) {
        AiEvaluationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("AI evaluation job {} not found for DLQ, skipping", jobId);
            return;
        }
        job.setErrorMessage(error);
        job.setStatus(AiEvaluationJob.STATUS_FAILED);
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
                SectionCitationReviewResponse review = sectionCitationReviewService.run(
                        documentId,
                        projectId,
                        sectionId,
                        payload.path("contentFingerprint").asText(),
                        requestedByUserId);
                EvidenceTraceService.RoundMaterialization materialization =
                        evidenceTraceService.materialize(
                                documentId, sectionId, requestedByUserId, review);
                if (materialization.recheckRequired()) {
                    submitTraceRecheck(
                            projectId,
                            materialization.previousRoundId(),
                            materialization.roundId());
                }
                yield objectMapper.valueToTree(review);
            }
            case AiEvaluationJob.KIND_SECTION_SUGGESTION -> {
                UUID documentId = UUID.fromString(payload.path("documentId").asText());
                UUID projectId = UUID.fromString(payload.path("projectId").asText());
                UUID sectionId = UUID.fromString(payload.path("sectionId").asText());
                String sectionType = payload.path("sectionType").asText();
                if (!job.getProjectId().equals(projectId)) {
                    throw new IllegalArgumentException(
                            "Section suggestion payload project does not match its job");
                }
                PaperSection section = paperSectionRepository.findByIdWithDocument(sectionId)
                        .filter(found -> found.getDocument() != null)
                        .filter(found -> documentId.equals(found.getDocument().getId()))
                        .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
                if (section.getContentTex() == null || section.getContentTex().isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Section Suggestions require a non-empty saved section");
                }
                yield sectionSuggestionResult(section, job.getProjectId(), sectionType);
            }
            case AiEvaluationJob.KIND_SOURCE_MATCHES -> runSourceMatches(job, payload);
            case AiEvaluationJob.KIND_TRACE_RECHECK -> {
                UUID projectId = UUID.fromString(payload.path("projectId").asText());
                if (!job.getProjectId().equals(projectId)) {
                    throw new IllegalArgumentException(
                            "Trace recheck payload project does not match its job");
                }
                UUID previousRoundId = UUID.fromString(payload.path("previousRoundId").asText());
                UUID linkedRoundId = UUID.fromString(payload.path("linkedRoundId").asText());
                int rechecked = evidenceTraceService.recheck(
                        projectId, previousRoundId, linkedRoundId);
                yield objectMapper.valueToTree(Map.of(
                        "previousRoundId", previousRoundId,
                        "linkedRoundId", linkedRoundId,
                        "rechecked", rechecked));
            }
            default -> throw new IllegalStateException("Unknown AI evaluation job kind: " + job.getKind());
        };
    }

    private void submitTraceRecheck(
            UUID projectId, UUID previousRoundId, UUID linkedRoundId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "projectId", projectId,
                    "previousRoundId", previousRoundId,
                    "linkedRoundId", linkedRoundId));
            submit(projectId, AiEvaluationJob.KIND_TRACE_RECHECK, payload);
        } catch (Exception exception) {
            log.warn(
                    "Citation Review round {} succeeded, but TRACE_RECHECK could not be queued: {}",
                    linkedRoundId,
                    exception.getMessage());
        }
    }

    private JsonNode runSourceMatches(AiEvaluationJob job, JsonNode payload) throws Exception {
        UUID documentId = UUID.fromString(payload.path("documentId").asText());
        UUID projectId = UUID.fromString(payload.path("projectId").asText());
        UUID sectionId = UUID.fromString(payload.path("sectionId").asText());
        if (!job.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException(
                    "Source matches payload project does not match its job");
        }
        SectionReviewSourceMatchRequest request = objectMapper.treeToValue(
                payload.get("findings"), SectionReviewSourceMatchRequest.class);
        return objectMapper.valueToTree(
                sectionCitationReviewService.sourceMatches(documentId, sectionId, request));
    }

    private JsonNode sectionSuggestionResult(PaperSection section, UUID projectId, String sectionType)
            throws Exception {
        ReviewGuide guide = reviewGuideRepository.findById(sectionType)
                .or(() -> reviewGuideRepository.findById("DEFAULT"))
                .orElseThrow(() -> new IllegalStateException(
                        "No review guide exists for section type: " + sectionType));
        List<String> checklist = parseChecklist(guide.getChecklistJson());
        List<SectionCitationReviewService.RetrievedEvidence> evidence =
                sectionCitationReviewService.retrieveEvidence(projectId, section.getContentTex());
        log.info("Section suggestion job for section {} (type '{}') matched guide '{}' with {} checklist items",
                section.getId(), sectionType, guide.getSectionType(), checklist.size());
        AiModelClient.GenerationResult generation = null;
        try {
            generation = aiModelClient.generate(
                    SectionSuggestionPrompt.SYSTEM,
                    SectionSuggestionPrompt.build(
                            guide.getSectionType(), checklist, section.getContentTex(), evidence));
            JsonNode root = objectMapper.readTree(extractJsonArray(generation.response()));
            validateSuggestionEvidence(root, evidence);
            List<SectionSuggestionDto> suggestions = objectMapper.convertValue(
                    root, new TypeReference<>() {
                    });
            log.info("Section suggestion LLM output for section {}: {}",
                    section.getId(), truncate(generation.response()));
            return objectMapper.valueToTree(suggestions);
        } catch (AiModelClient.AiApiException exception) {
            // Upstream model service failed (429/502/503/504 after retries): preserve the
            // real status so the client sees the actual failure instead of a generic 502.
            throw exception;
        } catch (Exception exception) {
            log.warn("Section suggestion job for {} produced invalid AI output: {}",
                    projectId, generation != null ? generation.response() : "no response");
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "AI returned invalid section suggestions", exception);
        }
    }

    private void validateSuggestionEvidence(
            JsonNode root, List<SectionCitationReviewService.RetrievedEvidence> evidence) {
        for (JsonNode item : root) {
            JsonNode evidenceNode = item.path("evidence");
            if (evidenceNode.isNull() || evidenceNode.isMissingNode()) {
                continue;
            }
            String chunkId = evidenceNode.path("chunk_id").asText("");
            if (chunkId.isEmpty()) {
                continue;
            }
            boolean known = evidence.stream()
                    .anyMatch(entry -> entry.chunkId().toString().equals(chunkId));
            if (!known) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Suggestion referenced unknown chunk_id " + chunkId);
            }
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 2_000 ? value.substring(0, 2_000) + "..." : value;
    }

    private List<String> parseChecklist(String checklistJson) {
        if (checklistJson == null || checklistJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(checklistJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String extractJsonArray(String response) {
        if (response == null) {
            throw new IllegalArgumentException("Empty AI response");
        }
        String stripped = response.replaceAll("(?s)```(?:json)?|```", "");
        int start = stripped.indexOf('[');
        int end = stripped.lastIndexOf(']');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("AI response did not contain a JSON array");
        }
        return stripped.substring(start, end + 1);
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
