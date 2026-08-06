package com.evidencepilot.service.impl;

import com.evidencepilot.client.ai.config.SectionAuditProperties;
import com.evidencepilot.dto.ai.AuditedSnippet;
import com.evidencepilot.dto.ai.SectionAuditFlags;
import com.evidencepilot.dto.ai.SectionContextRequest;
import com.evidencepilot.dto.ai.SectionContextResponse;
import com.evidencepilot.dto.response.SectionAuditFindingResponse;
import com.evidencepilot.dto.response.SectionAuditResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.SectionAuditFinding;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.SectionAuditFindingStatus;
import com.evidencepilot.model.enums.SectionAuditIssueType;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.SectionAuditFindingRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.AuditService;
import com.evidencepilot.service.PaperStandardService;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
public class SectionAuditService {

    private static final String PROMPT_VERSION = "section-audit-prompt-v2";
    private static final int MAX_CHUNK = 8_000;

    private final AiModelClient aiModelClient;
    private final PaperSectionRepository paperSectionRepository;
    private final SectionAuditFindingRepository sectionAuditFindingRepository;
    private final UserRepository userRepository;
    private final PaperStandardService paperStandardService;
    private final AuditService auditService;
    private final SectionAuditProperties sectionAuditProperties;
    private final Semaphore aiRequestLimiter;

    @Transactional
    public SectionAuditResponse run(
            UUID projectId,
            UUID sectionId,
            String expectedFingerprint,
            UUID requestedByUserId) {
        PaperSection section = requireSection(projectId, sectionId, true);
        Project project = section.getDocument().getProject();
        String fingerprint = fingerprint(section.getContentTex());
        if (!fingerprint.equals(expectedFingerprint)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "SECTION_CONTENT_CHANGED: save the section and run AI audit again");
        }
        List<SectionAuditFinding> existing = sectionAuditFindingRepository
                .findBySectionIdAndContentFingerprint(sectionId, fingerprint);
        if (!existing.isEmpty()) {
            return toResponse(sectionId, fingerprint, existing);
        }
        // ponytail: a clean section with zero findings is re-audited on every run;
        // add a zero-findings marker row if the repeated AI cost ever shows up.
        User actor = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException(requestedByUserId, "User"));

        String normalizedTitle = paperStandardService.normalizeSectionTitle(section.getSectionTitle());
        if (isReferenceSection(normalizedTitle)) {
            auditService.record("AI_SECTION_AUDIT", "PaperSection", sectionId, actor, null,
                    "skipped, references section");
            return toResponse(sectionId, fingerprint, List.of());
        }

        List<SectionAuditFinding> rows = new ArrayList<>();
        for (Chunk chunk : chunks(section.getContentTex())) {
            rows.addAll(auditChunk(project, section, actor, fingerprint, chunk));
        }
        // Append-only: older findings stay as stale history, filtered out by fingerprint.
        List<SectionAuditFinding> saved = sectionAuditFindingRepository.saveAll(rows);

        auditService.record("AI_SECTION_AUDIT", "PaperSection", sectionId, actor, null,
                saved.size() + " findings");
        return toResponse(sectionId, fingerprint, saved);
    }

    @Transactional(readOnly = true)
    public List<SectionAuditFindingResponse> findings(UUID sectionId, SectionAuditFindingStatus status) {
        PaperSection section = paperSectionRepository.findById(sectionId).orElse(null);
        if (section == null || section.getContentTex() == null || section.getContentTex().isBlank()) {
            return List.of();
        }
        String fingerprint = fingerprint(section.getContentTex());
        List<SectionAuditFinding> rows = status == null
                ? sectionAuditFindingRepository
                        .findBySectionIdAndContentFingerprintOrderByStartIndexAsc(sectionId, fingerprint)
                : sectionAuditFindingRepository
                        .findBySectionIdAndContentFingerprintAndStatusOrderByStartIndexAsc(
                                sectionId, fingerprint, status);
        return rows.stream().map(SectionAuditFindingResponse::from).toList();
    }

    @Transactional
    public SectionAuditFindingResponse updateStatus(UUID findingId, SectionAuditFindingStatus newStatus, UUID requestedByUserId) {
        if (newStatus == null || newStatus == SectionAuditFindingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status must be RESOLVED or DISMISSED");
        }
        SectionAuditFinding finding = sectionAuditFindingRepository.findById(findingId)
                .orElseThrow(() -> new ResourceNotFoundException(findingId, "SectionAuditFinding"));
        if (finding.getStatus() != SectionAuditFindingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Finding is already " + finding.getStatus());
        }
        finding.setStatus(newStatus);
        finding.setUpdatedAt(LocalDateTime.now());
        SectionAuditFinding saved = sectionAuditFindingRepository.save(finding);

        userRepository.findById(requestedByUserId).ifPresent(actor -> auditService.record(
                "SECTION_AUDIT_FINDING_STATUS",
                "SectionAuditFinding",
                findingId,
                actor,
                SectionAuditFindingStatus.PENDING.name(),
                newStatus.name()));
        return SectionAuditFindingResponse.from(saved);
    }

    private List<SectionAuditFinding> auditChunk(
            Project project,
            PaperSection section,
            User actor,
            String fingerprint,
            Chunk chunk) {
        SectionContextRequest request = new SectionContextRequest(
                section.getSectionTitle(),
                section.getContentTex(),
                chunk.content(),
                sectionAuditProperties.getEvaluationCriteria(),
                new SectionAuditFlags(sectionAuditProperties.isRequiresCitationCheck()));
        try {
            aiRequestLimiter.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Interrupted while waiting for AI capacity", exception);
        }
        try {
            SectionContextResponse response = aiModelClient.auditSection(request);
            if (response == null || response.snippets() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an invalid section audit");
            }
            List<SectionAuditFinding> rows = new ArrayList<>();
            for (AuditedSnippet snippet : response.snippets()) {
                rows.add(newFinding(project, section, actor, fingerprint, chunk, anchor(chunk, snippet), snippet));
            }
            return rows;
        } finally {
            aiRequestLimiter.release();
        }
    }

    private AnchoredSnippet anchor(Chunk chunk, AuditedSnippet snippet) {
        String text = snippet.originalTextSnippet();
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned a snippet without text");
        }
        int start = snippet.startIndex();
        int end = snippet.endIndex();
        if (start < 0 || end <= start || end > chunk.content().length()
                || !chunk.content().substring(start, end).equals(text)) {
            // ponytail: Python indexes are Unicode code points, Java indexes are UTF-16 units
            // (an emoji is 2 units); re-anchor on the raw text when they disagree.
            start = chunk.content().indexOf(text);
            if (start < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "AI returned a snippet not present in the supplied chunk");
            }
            end = start + text.length();
        }
        return new AnchoredSnippet(start, end, snippet);
    }

    private SectionAuditFinding newFinding(
            Project project,
            PaperSection section,
            User actor,
            String fingerprint,
            Chunk chunk,
            AnchoredSnippet anchored,
            AuditedSnippet snippet) {
        SectionAuditFinding row = new SectionAuditFinding();
        row.setProject(project);
        row.setSection(section);
        row.setContentFingerprint(fingerprint);
        row.setStartIndex(chunk.startOffset() + anchored.startIndex());
        row.setEndIndex(chunk.startOffset() + anchored.endIndex());
        row.setOriginalTextSnippet(snippet.originalTextSnippet());
        row.setIssueType(toIssueType(snippet.issueType()));
        row.setSuggestedParaphrase(snippet.suggestedParaphrase());
        row.setRationale(snippet.rationale());
        row.setStatus(SectionAuditFindingStatus.PENDING);
        row.setCreatedBy(actor);
        row.setModelName(null);
        row.setPromptVersion(PROMPT_VERSION);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(row.getCreatedAt());
        return row;
    }

    private static SectionAuditIssueType toIssueType(String issueType) {
        try {
            return SectionAuditIssueType.valueOf(issueType);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "AI returned an invalid section audit issue type", exception);
        }
    }

    private PaperSection requireSection(UUID projectId, UUID sectionId, boolean requireContent) {
        PaperSection section = paperSectionRepository.findByIdWithDocument(sectionId)
                .filter(PaperSection::isActive)
                .filter(found -> found.getDocument().isActive())
                .filter(found -> found.getDocument().getDocType() == DocumentType.PAPER)
                .filter(found -> found.getDocument().getProject() != null)
                .filter(found -> projectId.equals(found.getDocument().getProject().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(sectionId, "PaperSection"));
        if (requireContent && (section.getContentTex() == null || section.getContentTex().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Section audit requires a non-empty saved section");
        }
        return section;
    }

    private static boolean isReferenceSection(String title) {
        return "References".equals(title) || "Works Cited".equals(title);
    }

    public static String fingerprint(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * Semantic chunking: blank-line paragraphs stay whole (up to {@link #MAX_CHUNK} chars);
     * oversized paragraphs are split on sentence boundaries; unbreakable runs are hard-cut.
     */
    private static List<Chunk> chunks(String content) {
        List<Chunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int sep = nextParagraphEnd(content, start);
            int end = sep < 0 ? content.length() : sep;
            addParagraphChunks(content, start, end, chunks);
            start = sep < 0 ? content.length() : sep + (content.charAt(sep) == '\r' ? 4 : 2);
        }
        return chunks;
    }

    private static int nextParagraphEnd(String content, int from) {
        int lf = content.indexOf("\n\n", from);
        int crlf = content.indexOf("\r\n\r\n", from);
        if (lf < 0) {
            return crlf;
        }
        if (crlf < 0) {
            return lf;
        }
        return Math.min(lf, crlf);
    }

    private static void addParagraphChunks(String content, int start, int end, List<Chunk> out) {
        if (end <= start || content.substring(start, end).isBlank()) {
            return;
        }
        if (end - start <= MAX_CHUNK) {
            out.add(new Chunk(start, content.substring(start, end)));
            return;
        }
        int cursor = start;
        while (cursor < end) {
            int limit = Math.min(end, cursor + MAX_CHUNK);
            int cut = -1;
            for (int i = limit - 1; i > cursor; i--) {
                char c = content.charAt(i);
                if ((c == '.' || c == '!' || c == '?')
                        && i + 1 < end
                        && Character.isWhitespace(content.charAt(i + 1))) {
                    cut = i + 1;
                    break;
                }
            }
            if (cut < 0) {
                // ponytail: no sentence boundary inside the window, hard-cut the long run
                cut = limit;
            }
            if (!content.substring(cursor, cut).isBlank()) {
                out.add(new Chunk(cursor, content.substring(cursor, cut)));
            }
            cursor = cut;
        }
    }

    private SectionAuditResponse toResponse(UUID sectionId, String fingerprint, List<SectionAuditFinding> rows) {
        return new SectionAuditResponse(
                sectionId,
                fingerprint,
                rows.stream().map(SectionAuditFindingResponse::from).toList());
    }

    private record Chunk(int startOffset, String content) {
    }

    private record AnchoredSnippet(int startIndex, int endIndex, AuditedSnippet snippet) {
    }
}
