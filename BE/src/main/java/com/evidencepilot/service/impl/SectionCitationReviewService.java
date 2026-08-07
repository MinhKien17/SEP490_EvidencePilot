package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.SectionReviewSourceMatchRequest;
import com.evidencepilot.dto.response.SectionCitationReviewResponse;
import com.evidencepilot.dto.response.SectionReviewSourceMatchesResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ReviewSnapshot;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.prompt.SectionCitationReviewPrompt;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ReviewSnapshotRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.AuditService;
import com.evidencepilot.service.PaperStandardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SectionCitationReviewService {

    public static final String REVIEW_VERSION = "section-citation-v1";
    public static final String RULE_CATALOG_VERSION = "citation-rules-v1";
    private static final String SNAPSHOT_STYLE = REVIEW_VERSION;
    private static final int CHUNK_SIZE = 8_000;
    private static final int CHUNK_OVERLAP = 400;
    private static final int MAX_FINDINGS = 10;
    private static final int SOURCE_TOP_K = 20;
    private static final int SOURCE_LIMIT = 3;
    private final AiModelClient aiModelClient;
    private final PaperSectionRepository paperSectionRepository;
    private final ReviewSnapshotRepository reviewSnapshotRepository;
    private final UserRepository userRepository;
    private final PaperStandardService paperStandardService;
    private final SourceMatchingService sourceMatchingService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<SectionCitationReviewResponse> cached(UUID documentId, UUID sectionId) {
        PaperSection section = requireSection(documentId, sectionId, false);
        String fingerprint = fingerprint(section);
        return reviewSnapshotRepository
                .findByProjectIdAndStyleAndInputFingerprint(
                        section.getDocument().getProject().getId(), SNAPSHOT_STYLE, fingerprint)
                .flatMap(this::readSnapshot);
    }

    @Transactional
    public SectionCitationReviewResponse run(
            UUID documentId,
            UUID projectId,
            UUID sectionId,
            String expectedFingerprint,
            UUID requestedByUserId) {
        PaperSection section = requireSection(documentId, sectionId, true);
        Project project = section.getDocument().getProject();
        if (!projectId.equals(project.getId())) {
            throw new IllegalArgumentException("Section review project does not match its job");
        }
        String fingerprint = fingerprint(section);
        if (!fingerprint.equals(expectedFingerprint)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "SECTION_CONTENT_CHANGED: save the section and run Citation Review again");
        }
        Optional<SectionCitationReviewResponse> cached = reviewSnapshotRepository
                .findByProjectIdAndStyleAndInputFingerprint(projectId, SNAPSHOT_STYLE, fingerprint)
                .flatMap(this::readSnapshot);
        if (cached.isPresent()) {
            return cached.get();
        }

        String normalizedTitle = paperStandardService.normalizeSectionTitle(section.getSectionTitle());
        SectionCitationReviewResponse review = isReferenceSection(normalizedTitle)
                ? notApplicable(section, fingerprint)
                : generate(section, fingerprint, normalizedTitle);
        saveSnapshot(project, fingerprint, review);

        User actor = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException(requestedByUserId, "User"));
        auditService.record(
                "AI_SECTION_CITATION_REVIEW",
                "PaperSection",
                sectionId,
                actor,
                null,
                review);
        return review;
    }

    @Transactional(readOnly = true)
    public SectionReviewSourceMatchesResponse sourceMatches(
            UUID documentId,
            UUID sectionId,
            SectionReviewSourceMatchRequest request) {
        PaperSection section = requireSection(documentId, sectionId, true);
        List<SectionReviewSourceMatchRequest.Finding> findings = request.findings();
        if (findings == null || findings.isEmpty() || findings.size() > MAX_FINDINGS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Provide between 1 and 10 review findings");
        }

        String content = section.getContentTex();
        Set<Integer> indexes = new LinkedHashSet<>();
        for (SectionReviewSourceMatchRequest.Finding finding : findings) {
            if (!indexes.add(finding.findingIndex())
                    || finding.startOffset() < 0
                    || finding.endOffset() <= finding.startOffset()
                    || finding.endOffset() > content.length()
                    || !content.substring(finding.startOffset(), finding.endOffset())
                            .equals(finding.excerpt())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A review finding no longer matches the saved section; run Citation Review again");
            }
        }

        List<List<SourceMatchingService.SourceMatch>> matches = sourceMatchingService.search(
                section.getDocument().getProject().getId(),
                findings.stream().map(SectionReviewSourceMatchRequest.Finding::excerpt).toList(),
                SOURCE_TOP_K);
        List<SectionReviewSourceMatchesResponse.FindingMatches> response = new ArrayList<>();
        for (int i = 0; i < findings.size(); i++) {
            Map<UUID, SectionReviewSourceMatchesResponse.SourceCandidate> unique = new LinkedHashMap<>();
            for (SourceMatchingService.SourceMatch match : matches.get(i)) {
                Document source = match.chunk().getDocument();
                unique.putIfAbsent(source.getId(), toCandidate(match));
                if (unique.size() == SOURCE_LIMIT) {
                    break;
                }
            }
            response.add(new SectionReviewSourceMatchesResponse.FindingMatches(
                    findings.get(i).findingIndex(), List.copyOf(unique.values())));
        }
        return new SectionReviewSourceMatchesResponse(response);
    }

    public String fingerprint(PaperSection section) {
        Project project = section.getDocument().getProject();
        String standard = project.getTargetStandard() == null
                ? "CUSTOM" : project.getTargetStandard().name();
        String input = REVIEW_VERSION + '\0' + RULE_CATALOG_VERSION + '\0' + SectionCitationReviewPrompt.SYSTEM
                + '\0' + standard + '\0' + section.getId() + '\0' + section.getSectionTitle()
                + '\0' + section.getContentTex();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private SectionCitationReviewResponse generate(
            PaperSection section,
            String fingerprint,
            String normalizedTitle) {
        List<Chunk> chunks = chunks(section.getContentTex());
        List<SectionCitationReviewResponse.Finding> findings = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        List<String> limitations = new ArrayList<>();
        String provider = null;
        String model = null;
        RuntimeException lastFailure = null;
        int completedChunks = 0;

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            try {
                GeneratedReview generated = generateChunkReview(
                        section, normalizedTitle, chunk, i, chunks.size());
                if (provider == null) {
                    provider = generated.provider();
                    model = generated.model();
                }
                if (generated.review().summary() != null
                        && !generated.review().summary().isBlank()) {
                    summaries.add(generated.review().summary().strip());
                }
                for (ModelFinding finding : generated.review().findings()) {
                    int start = chunk.startOffset() + finding.startOffset();
                    int end = chunk.startOffset() + finding.endOffset();
                    if (!alreadyCited(section.getContentTex(), finding.excerpt(), end)) {
                        findings.add(new SectionCitationReviewResponse.Finding(
                                finding.ruleCode(),
                                finding.excerpt(),
                                start,
                                end,
                                finding.reason().strip(),
                                finding.recommendedAction().strip()));
                    }
                }
                completedChunks++;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                limitations.add("Chunk " + (i + 1) + "/" + chunks.size()
                        + " could not be reviewed: " + exception.getMessage());
            }
        }
        if (completedChunks == 0 && lastFailure != null) {
            throw lastFailure;
        }

        Map<String, SectionCitationReviewResponse.Finding> unique = new LinkedHashMap<>();
        findings.stream()
                .sorted(Comparator.comparingInt(SectionCitationReviewResponse.Finding::startOffset))
                .forEach(finding -> unique.putIfAbsent(
                        finding.ruleCode() + ":" + finding.startOffset() + ":" + finding.endOffset(),
                        finding));
        List<SectionCitationReviewResponse.Finding> prioritized = unique.values().stream()
                .limit(MAX_FINDINGS)
                .toList();
        String summary = String.join(" ", summaries);
        if (summary.length() > 1_200) {
            summary = summary.substring(0, 1_197) + "...";
        }
        return new SectionCitationReviewResponse(
                REVIEW_VERSION,
                RULE_CATALOG_VERSION,
                section.getId(),
                section.getVersion(),
                fingerprint,
                LocalDateTime.now(),
                provider,
                model,
                completedChunks == chunks.size(),
                summary,
                prioritized,
                limitations);
    }

    private GeneratedReview generateChunkReview(
            PaperSection section,
            String normalizedTitle,
            Chunk chunk,
            int chunkIndex,
            int chunkCount) {
        String prompt = reviewPrompt(section, normalizedTitle, chunk, chunkIndex, chunkCount);
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                AiModelClient.GenerationResult generation = aiModelClient.generate(
                        SectionCitationReviewPrompt.SYSTEM,
                        attempt == 0 ? prompt : prompt + "\nPrevious output was invalid. Return valid JSON only.");
                ModelReview review = strictMapper().readValue(
                        extractJson(generation.response()), ModelReview.class);
                validateReview(review, allowedRules(normalizedTitle), chunk);
                return new GeneratedReview(generation.provider(), generation.model(), review);
            } catch (JsonProcessingException | IllegalArgumentException exception) {
                lastFailure = new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI returned an invalid section citation review",
                        exception);
            }
        }
        throw lastFailure;
    }

    private String reviewPrompt(
            PaperSection section,
            String normalizedTitle,
            Chunk chunk,
            int chunkIndex,
            int chunkCount) {
        Map<String, Object> context = new LinkedHashMap<>();
        Project project = section.getDocument().getProject();
        context.put("paperStandard", project.getTargetStandard() == null
                ? "CUSTOM" : project.getTargetStandard().name());
        context.put("sectionId", section.getId());
        context.put("sectionTitle", section.getSectionTitle());
        context.put("normalizedSectionTitle", normalizedTitle);
        context.put("applicableRules", allowedRules(normalizedTitle));
        context.put("sectionPolicy", sectionPolicy(normalizedTitle));
        context.put("chunkIndex", chunkIndex);
        context.put("chunkCount", chunkCount);
        context.put("contentTex", chunk.content());
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize section review context", exception);
        }
    }

    private void validateReview(
            ModelReview review,
            Set<SectionCitationReviewResponse.RuleCode> allowedRules,
            Chunk chunk) {
        if (review == null || review.findings() == null || review.findings().size() > MAX_FINDINGS) {
            throw new IllegalArgumentException("Invalid findings collection");
        }
        for (ModelFinding finding : review.findings()) {
            if (finding == null
                    || finding.ruleCode() == null
                    || !allowedRules.contains(finding.ruleCode())
                    || finding.excerpt() == null
                    || finding.excerpt().isBlank()
                    || finding.reason() == null
                    || finding.reason().isBlank()
                    || finding.recommendedAction() == null
                    || finding.recommendedAction().isBlank()
                    || finding.startOffset() < 0
                    || finding.endOffset() <= finding.startOffset()
                    || finding.endOffset() > chunk.content().length()
                    || !chunk.content().substring(finding.startOffset(), finding.endOffset())
                            .equals(finding.excerpt())) {
                throw new IllegalArgumentException("Finding is not grounded in the supplied chunk");
            }
        }
    }

    private Set<SectionCitationReviewResponse.RuleCode> allowedRules(String title) {
        return switch (title) {
            case "Abstract", "References", "Works Cited" -> EnumSet.noneOf(
                    SectionCitationReviewResponse.RuleCode.class);
            case "Methodology" -> EnumSet.of(
                    SectionCitationReviewResponse.RuleCode.EXTERNAL_FACT_OR_DEFINITION,
                    SectionCitationReviewResponse.RuleCode.QUANTITATIVE_OR_STATISTICAL_CLAIM,
                    SectionCitationReviewResponse.RuleCode.ATTRIBUTED_METHOD_DATASET_OR_STANDARD);
            case "Results" -> EnumSet.of(
                    SectionCitationReviewResponse.RuleCode.EXTERNAL_FACT_OR_DEFINITION,
                    SectionCitationReviewResponse.RuleCode.QUANTITATIVE_OR_STATISTICAL_CLAIM,
                    SectionCitationReviewResponse.RuleCode.PRIOR_WORK_OR_COMPARISON,
                    SectionCitationReviewResponse.RuleCode.ATTRIBUTED_METHOD_DATASET_OR_STANDARD);
            case "Discussion" -> EnumSet.of(
                    SectionCitationReviewResponse.RuleCode.EXTERNAL_FACT_OR_DEFINITION,
                    SectionCitationReviewResponse.RuleCode.QUANTITATIVE_OR_STATISTICAL_CLAIM,
                    SectionCitationReviewResponse.RuleCode.PRIOR_WORK_OR_COMPARISON,
                    SectionCitationReviewResponse.RuleCode.CAUSAL_OR_GENERALIZABLE_CLAIM);
            case "Conclusion" -> EnumSet.of(
                    SectionCitationReviewResponse.RuleCode.EXTERNAL_FACT_OR_DEFINITION,
                    SectionCitationReviewResponse.RuleCode.QUANTITATIVE_OR_STATISTICAL_CLAIM,
                    SectionCitationReviewResponse.RuleCode.PRIOR_WORK_OR_COMPARISON,
                    SectionCitationReviewResponse.RuleCode.CAUSAL_OR_GENERALIZABLE_CLAIM);
            default -> EnumSet.allOf(SectionCitationReviewResponse.RuleCode.class);
        };
    }

    private String sectionPolicy(String title) {
        return switch (title) {
            case "Abstract" -> "Do not request citations by default; return no findings.";
            case "Methodology" -> "Flag named or adapted methods, datasets, instruments, standards, and external factual assertions.";
            case "Results" -> "Do not flag current-study results; flag only external baselines, comparisons, methods, or facts.";
            case "Discussion" -> "Flag prior-work comparisons, external theories or facts, statistics, and causal generalizations.";
            case "Conclusion" -> "Flag only new external facts, prior work, statistics, or generalizations, not summaries of this study.";
            default -> "Flag external facts, statistics, prior work, named methods or standards, and causal generalizations.";
        };
    }

    private PaperSection requireSection(UUID documentId, UUID sectionId, boolean requireContent) {
        PaperSection section = paperSectionRepository.findByIdWithDocument(sectionId)
                .filter(PaperSection::isActive)
                .filter(found -> documentId.equals(found.getDocument().getId()))
                .filter(found -> found.getDocument().isActive())
                .filter(found -> found.getDocument().getDocType() == DocumentType.PAPER)
                .filter(found -> found.getDocument().getProject() != null)
                .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
        if (requireContent && (section.getContentTex() == null || section.getContentTex().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Citation Review requires a non-empty saved section");
        }
        return section;
    }

    private Optional<SectionCitationReviewResponse> readSnapshot(ReviewSnapshot snapshot) {
        try {
            return Optional.of(objectMapper.readValue(
                    snapshot.getResponseJson(), SectionCitationReviewResponse.class));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private void saveSnapshot(
            Project project,
            String fingerprint,
            SectionCitationReviewResponse review) {
        ReviewSnapshot snapshot = new ReviewSnapshot();
        snapshot.setProject(project);
        snapshot.setStyle(SNAPSHOT_STYLE);
        snapshot.setInputFingerprint(fingerprint);
        snapshot.setCreatedAt(LocalDateTime.now());
        try {
            snapshot.setResponseJson(objectMapper.writeValueAsString(review));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize section review", exception);
        }
        reviewSnapshotRepository.save(snapshot);
    }

    private SectionCitationReviewResponse notApplicable(PaperSection section, String fingerprint) {
        return new SectionCitationReviewResponse(
                REVIEW_VERSION,
                RULE_CATALOG_VERSION,
                section.getId(),
                section.getVersion(),
                fingerprint,
                LocalDateTime.now(),
                null,
                null,
                true,
                "Citation review is not applicable to the references section.",
                List.of(),
                List.of());
    }

    private SectionReviewSourceMatchesResponse.SourceCandidate toCandidate(
            SourceMatchingService.SourceMatch match) {
        Document source = match.chunk().getDocument();
        String filename = source.getOriginalFilename() == null || source.getOriginalFilename().isBlank()
                ? source.getId().toString() : source.getOriginalFilename();
        String title = source.getTitle() == null || source.getTitle().isBlank()
                ? filename : source.getTitle();
        return new SectionReviewSourceMatchesResponse.SourceCandidate(
                match.chunk().getId(),
                source.getId(),
                SourceMatchingService.citationKey(source.getId()),
                filename,
                title,
                source.getAuthors(),
                source.getPublicationYear(),
                source.getDoi(),
                match.chunk().getText(),
                match.similarityScore());
    }

    private static List<Chunk> chunks(String content) {
        List<Chunk> chunks = new ArrayList<>();
        for (int start = 0; start < content.length();) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            chunks.add(new Chunk(start, content.substring(start, end)));
            if (end == content.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    private static boolean alreadyCited(String content, String excerpt, int endOffset) {
        if (excerpt.contains("\\cite")) {
            return true;
        }
        String suffix = content.substring(endOffset, Math.min(content.length(), endOffset + 80));
        return suffix.stripLeading().startsWith("\\cite");
    }

    private ObjectMapper strictMapper() {
        return objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
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

    private static boolean isReferenceSection(String title) {
        return "References".equals(title) || "Works Cited".equals(title);
    }

    private record Chunk(int startOffset, String content) {
    }

    private record GeneratedReview(String provider, String model, ModelReview review) {
    }

    private record ModelReview(String summary, List<ModelFinding> findings) {
    }

    private record ModelFinding(
            SectionCitationReviewResponse.RuleCode ruleCode,
            String excerpt,
            int startOffset,
            int endOffset,
            String reason,
            String recommendedAction) {
    }
}
