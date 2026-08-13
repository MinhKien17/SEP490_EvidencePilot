package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.TraceDecisionRequest;
import com.evidencepilot.dto.request.TraceReviewRequest;
import com.evidencepilot.dto.response.EvidenceTraceResponse;
import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.CitationReviewRound;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.EvidenceRevisionTrace;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.InstructorJudgment;
import com.evidencepilot.model.enums.TraceOutcome;
import com.evidencepilot.prompt.TraceRecheckPrompt;
import com.evidencepilot.repository.CitationReviewRoundRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.EvidenceRevisionTraceRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.CurrentUserService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceTraceService {

    private static final int PASSAGE_RADIUS = 120;
    private static final int EVIDENCE_TEXT_LIMIT = 1_200;
    private static final int MAX_RECHECK_PER_RUN = 10;

    private final CitationReviewRoundRepository roundRepository;
    private final EvidenceRevisionTraceRepository traceRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final SectionCitationReviewService sectionCitationReviewService;
    private final AiModelClient aiModelClient;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<EvidenceTraceResponse> materialize(
            UUID documentId,
            UUID sectionId,
            UUID requestedByUserId,
            SectionCitationReviewResponse review) {
        PaperSection section = paperSectionRepository.findByIdWithDocument(sectionId)
                .filter(found -> documentId.equals(found.getDocument().getId()))
                .filter(PaperSection::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
        if (roundRepository.existsBySectionIdAndContentFingerprint(
                sectionId, review.contentFingerprint())) {
            return tracesFor(sectionId, review);
        }
        User requestedBy = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException(requestedByUserId, "User"));
        CitationReviewRound round = new CitationReviewRound();
        round.setProject(section.getDocument().getProject());
        round.setSection(section);
        round.setSectionVersion(review.sectionVersion());
        round.setRequestedBy(requestedBy);
        round.setContentFingerprint(review.contentFingerprint());
        round.setStyle(review.reviewVersion());
        round.setGenerationMeta(generationMeta(review));
        round.setSummary(review.summary());
        round.setComplete(review.complete());
        round.setCreatedAt(LocalDateTime.now());
        round = roundRepository.save(round);

        List<EvidenceRevisionTrace> traces = new ArrayList<>();
        for (int index = 0; index < review.findings().size(); index++) {
            SectionCitationReviewResponse.Finding finding = review.findings().get(index);
            traces.add(toTrace(round, section, index, finding));
        }
        traceRepository.saveAll(traces);
        return traces.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EvidenceTraceResponse decide(
            UUID documentId,
            UUID sectionId,
            UUID traceId,
            TraceDecisionRequest request) {
        EvidenceRevisionTrace trace = traceRepository.findById(traceId)
                .filter(found -> sectionId.equals(found.getSection().getId()))
                .filter(found -> documentId.equals(found.getSection().getDocument().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(traceId, "EvidenceRevisionTrace"));
        PaperSection section = trace.getSection();
        String currentFingerprint = sectionCitationReviewService.fingerprint(section);
        if (!currentFingerprint.equals(trace.getRound().getContentFingerprint())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "SECTION_CONTENT_CHANGED: the section changed since this review; run Citation Review again");
        }
        // ponytail: blind substring around the frozen excerpt offsets; becomes
        // misaligned garbage after heavy rewrites. Upgrade path: diff against
        // project_checkpoints.snapshot_json instead of offset arithmetic.
        trace.setStudentAction(request.studentAction());
        trace.setExplanation(request.explanation());
        if (request.sourceId() != null) {
            trace.setSource(documentRepository.findById(request.sourceId()).orElse(null));
            trace.setSourceReplaced(true);
        }
        if (request.chunkId() != null) {
            trace.setChunk(documentChunkRepository.findById(request.chunkId()).orElse(null));
        }
        trace.setEvidenceQuote(request.evidenceQuote());
        trace.setEvidenceRelation(request.relation());
        trace.setAfterPassage(passageAround(section.getContentTex(),
                trace.getExcerptStart(), trace.getExcerptEnd()));
        trace.setAfterFingerprint(currentFingerprint);
        trace.setAfterSectionVersion(section.getVersion());
        trace.setOutcome(TraceOutcome.UNRESOLVED);
        return toResponse(traceRepository.save(trace));
    }

    @Transactional
    public void stampStaleOnContentChanged(UUID sectionId, String content, Integer version) {
        List<EvidenceRevisionTrace> open = traceRepository.findBySectionIdOrderByIdDesc(sectionId).stream()
                .filter(trace -> trace.getOutcome() == null || trace.getOutcome() == TraceOutcome.UNRESOLVED)
                .filter(trace -> trace.getJudgment() == null)
                .toList();
        for (EvidenceRevisionTrace trace : open) {
            trace.setOutcome(TraceOutcome.STALE);
            trace.setAfterPassage(passageAround(content, trace.getExcerptStart(), trace.getExcerptEnd()));
            trace.setAfterSectionVersion(version);
        }
        traceRepository.saveAll(open);
    }

    @Transactional
    public void recheck(UUID documentId, UUID sectionId, SectionCitationReviewResponse review) {
        PaperSection section = paperSectionRepository.findByIdWithDocument(sectionId)
                .filter(found -> documentId.equals(found.getDocument().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
        List<EvidenceRevisionTrace> candidates = traceRepository
                .findBySectionIdOrderByIdDesc(sectionId).stream()
                .filter(trace -> trace.getOutcome() == TraceOutcome.STALE)
                .filter(trace -> trace.getStudentAction() != null)
                .filter(trace -> trace.getAfterPassage() != null && !trace.getAfterPassage().isBlank())
                .limit(MAX_RECHECK_PER_RUN)
                .toList();
        for (EvidenceRevisionTrace trace : candidates) {
            try {
                recheckOne(section, trace);
            } catch (RuntimeException exception) {
                log.warn("Trace recheck {} failed: {}", trace.getId(), exception.getMessage());
            }
        }
    }

    private void recheckOne(PaperSection section, EvidenceRevisionTrace trace) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("sectionTitle", section.getSectionTitle());
        context.put("excerpt", trace.getExcerpt());
        context.put("rationale", trace.getRationale());
        context.put("evidence", trace.getEvidenceQuote() == null ? "" : trace.getEvidenceQuote());
        context.put("studentAction", trace.getStudentAction() == null ? null : trace.getStudentAction().name());
        context.put("studentExplanation", trace.getExplanation() == null ? "" : trace.getExplanation());
        context.put("revisedPassage", trace.getAfterPassage());
        String prompt;
        try {
            prompt = objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize trace recheck context", exception);
        }
        AiModelClient.GenerationResult generation = aiModelClient.generate(
                TraceRecheckPrompt.SYSTEM, prompt);
        RecheckVerdict verdict;
        try {
            verdict = recheckMapper().readValue(
                    extractJson(generation.response()), RecheckVerdict.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Recheck verdict is not valid JSON", exception);
        }
        if (verdict.judgment() == null) {
            throw new IllegalArgumentException("Recheck verdict missing judgment");
        }
        trace.setJudgment(verdict.judgment());
        trace.setOutcome(switch (verdict.judgment()) {
            case EFFECTIVE -> TraceOutcome.RESOLVED;
            case PARTIAL -> TraceOutcome.PARTIALLY_RESOLVED;
            case INEFFECTIVE -> TraceOutcome.UNRESOLVED;
        });
        traceRepository.save(trace);
    }

    @Transactional(readOnly = true)
    public List<EvidenceTraceResponse> listTraces(UUID projectId, TraceOutcome outcome) {
        List<EvidenceRevisionTrace> traces = outcome == null
                ? traceRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                : traceRepository.findByProjectIdAndOutcomeInOrderByCreatedAtDesc(
                        projectId, List.of(outcome));
        return traces.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EvidenceTraceResponse review(UUID projectId, UUID traceId, TraceReviewRequest request) {
        EvidenceRevisionTrace trace = traceRepository.findById(traceId)
                .filter(found -> projectId.equals(found.getRound().getProject().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(traceId, "EvidenceRevisionTrace"));
        User instructor = currentUserService.requireCurrentUser();
        trace.setInstructor(instructor);
        trace.setJudgment(request.judgment());
        trace.setInstructorFeedback(request.instructorFeedback());
        trace.setOutcome(TraceOutcome.RESOLVED);
        trace.setJudgedAt(LocalDateTime.now());
        return toResponse(traceRepository.save(trace));
    }

    public List<EvidenceTraceResponse> tracesFor(UUID sectionId, SectionCitationReviewResponse review) {
        return traceRepository.findBySectionIdOrderByIdDesc(sectionId).stream()
                .filter(trace -> review.contentFingerprint().equals(trace.getRound().getContentFingerprint()))
                .map(this::toResponse)
                .toList();
    }

    private EvidenceRevisionTrace toTrace(
            CitationReviewRound round,
            PaperSection section,
            int index,
            SectionCitationReviewResponse.Finding finding) {
        EvidenceRevisionTrace trace = new EvidenceRevisionTrace();
        trace.setRound(round);
        trace.setSection(section);
        trace.setFindingIndex(index);
        trace.setSuggestedAction(suggestedAction(finding.type()));
        trace.setExcerpt(finding.excerpt());
        trace.setExcerptStart(finding.startOffset());
        trace.setExcerptEnd(finding.endOffset());
        trace.setRationale(finding.rationale());
        trace.setConfidence(confidence(finding.confidence()));
        trace.setCreatedAt(LocalDateTime.now());
        SectionCitationReviewResponse.Evidence first = finding.evidence().isEmpty()
                ? null : finding.evidence().getFirst();
        if (first != null) {
            trace.setSource(documentRepository.findById(first.sourceId()).orElse(null));
            trace.setChunk(documentChunkRepository.findById(first.chunkId()).orElse(null));
            trace.setEvidenceQuote(first.quote());
            trace.setEvidenceRelation(first.relation() == null ? null : first.relation().name());
        }
        return trace;
    }

    private String suggestedAction(SectionCitationReviewResponse.FindingType type) {
        return type == SectionCitationReviewResponse.FindingType.UNSUBSTANTIATED_CLAIM
                ? "ADD_CITATION" : "QUALIFY";
    }

    private BigDecimal confidence(SectionCitationReviewResponse.Confidence confidence) {
        if (confidence == null) {
            return null;
        }
        return switch (confidence) {
            case HIGH -> new BigDecimal("1.0000");
            case MEDIUM -> new BigDecimal("0.6000");
            case LOW -> new BigDecimal("0.3000");
        };
    }

    private String generationMeta(SectionCitationReviewResponse review) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "reviewVersion", review.reviewVersion(),
                    "ruleCatalogVersion", review.ruleCatalogVersion(),
                    "provider", review.provider() == null ? "" : review.provider(),
                    "model", review.model() == null ? "" : review.model()));
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private EvidenceTraceResponse toResponse(EvidenceRevisionTrace trace) {
        String sourceTitle = null;
        Document source = trace.getSource();
        if (source != null) {
            sourceTitle = source.getTitle() == null || source.getTitle().isBlank()
                    ? source.getOriginalFilename() : source.getTitle();
        }
        return new EvidenceTraceResponse(
                trace.getId(),
                trace.getRound().getId(),
                trace.getSection().getId(),
                trace.getSection().getSectionTitle(),
                trace.getSection().getVersion(),
                trace.getFindingIndex(),
                trace.getSuggestedAction(),
                trace.getCriticality(),
                trace.getParentHeader(),
                trace.getExcerpt(),
                trace.getExcerptStart(),
                trace.getExcerptEnd(),
                trace.getRationale(),
                trace.getConfidence(),
                source == null ? null : source.getId(),
                sourceTitle,
                trace.getChunk() == null ? null : trace.getChunk().getId(),
                trace.getEvidenceQuote(),
                trace.getEvidenceRelation(),
                trace.getStudentAction(),
                trace.getExplanation(),
                trace.getAfterPassage(),
                trace.getAfterSectionVersion(),
                trace.getOutcome(),
                trace.getInstructor() == null ? null : trace.getInstructor().getId(),
                trace.getJudgment(),
                trace.getInstructorFeedback(),
                trace.getJudgedAt(),
                trace.getLinkedRound() == null ? null : trace.getLinkedRound().getId(),
                trace.getLinkedMode(),
                trace.getCreatedAt());
    }

    private static String passageAround(String content, int start, int end) {
        if (content == null || content.isBlank()) {
            return "";
        }
        int from = Math.max(0, start - PASSAGE_RADIUS);
        int to = Math.min(content.length(), end + PASSAGE_RADIUS);
        String passage = content.substring(from, to);
        return passage.length() > EVIDENCE_TEXT_LIMIT
                ? passage.substring(0, EVIDENCE_TEXT_LIMIT) : passage;
    }

    private ObjectMapper recheckMapper() {
        return objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }

    private static String extractJson(String response) {
        if (response == null) {
            throw new IllegalArgumentException("Empty AI response");
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("AI response did not contain JSON");
        }
        return response.substring(start, end + 1);
    }

    private record RecheckVerdict(
            @JsonProperty("judgment") InstructorJudgment judgment,
            @JsonProperty("reason") String reason) {
    }
}
