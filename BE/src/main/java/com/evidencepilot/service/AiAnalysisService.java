package com.evidencepilot.service;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.ai.AiModelClient.AiApiException;
import com.evidencepilot.ai.dto.*;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.EvidenceEdge;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.exception.AiValidationException;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.EvidenceEdgeRepository;
import com.evidencepilot.repository.SourceChunkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


/**
 * Orchestrates the full AI claim-analysis pipeline.
 *
 * <h2>Two-phase workflow</h2>
 * <ol>
 *   <li><b>Match</b> – calls {@code POST /match/claim} with the claim text to let
 *       the AI locate relevant source chunks it already knows about.  The best
 *       match supplies the {@code source_id} and {@code excerpt} required by the
 *       next step.</li>
 *   <li><b>Process</b> – calls {@code POST /process/claim} with the claim, the
 *       winning {@code source_id}, and its {@code excerpt}.  The response gives us
 *       a {@code verdict} enum, a numeric {@code confidence} score, and an
 *       {@code explanation}.</li>
 * </ol>
 *
 * <h2>Persistence</h2>
 * <ul>
 *   <li>{@code Claim.aiConfidenceScore} ← {@code confidence}</li>
 *   <li>{@code EvidenceEdge}             ← stores the structured analysis verdict, explanation, and missing evidence</li>
 * </ul>
 *
 * <p>Both writes happen inside a single {@link Transactional} boundary so a partial
 * failure cannot leave the DB in an inconsistent state.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AiModelClient aiModelClient;
    private final ClaimRepository claimRepository;
    private final EvidenceEdgeRepository evidenceEdgeRepository;
    private final SourceChunkRepository sourceChunkRepository;
    private final ClaimMatchingService claimMatchingService;
    private final ObjectMapper objectMapper;

    // ── Primary pipeline ───────────────────────────────────────────────────────

    /**
     * Full two-phase pipeline: match → process → persist.
     *
     * <p>Automatically discovers the best-matching source by calling
     * {@code POST /match/claim} first.  Use the overload below if the caller
     * already knows the exact {@code source_id} and {@code excerpt}.</p>
     *
     * @param claim the Claim to analyse (must already be persisted in the DB)
     * @return the updated Claim with {@code aiConfidenceScore} populated
     * @throws ResponseStatusException 404 if no matching sources are found in the AI system,
     *                                 502 on any AI API error
     */
    @Transactional
    public Claim analyzeAndPersist(Claim claim) {
        // ── Phase 1: find the best-matching source ─────────────────────────────
        log.info("Phase 1 – matching sources for claim {}", claim.getId());

        ClaimMatchResponse matchResponse = claimMatchingService.matchClaim(claim, 1);

        if (!matchResponse.hasMatches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No persisted source chunks found for claim " + claim.getId()
                    + ". Upload at least one source to this project before analysing.");
        }

        ClaimMatch topMatch = matchResponse.matches().get(0);
        log.info("Phase 1 complete – top match: sourceId={}, suitability={}, score={}",
                topMatch.sourceId(), topMatch.suitability(), topMatch.score());

        Integer chunkId = null;
        try {
            chunkId = Integer.valueOf(topMatch.chunkId());
        } catch (NumberFormatException ignored) {}

        // ── Phase 2: deep analysis with the winning source ─────────────────────
        return analyzeAndPersistWithChunk(claim, topMatch.sourceId(), chunkId, topMatch.excerpt(), null);
    }

    /**
     * Skips the match phase and calls {@code POST /process/claim} directly.
     *
     * <p>Use this overload when the caller already knows the {@code source_id}
     * (e.g. an instructor manually selecting a specific reference) and wants to
     * avoid the extra round-trip to the AI service.</p>
     *
     * @param claim    the Claim to analyse
     * @param sourceId the source identifier known to the AI service
     * @param excerpt  the relevant text excerpt from that source
     * @param title    optional title for the source (may be {@code null})
     * @return the updated Claim with {@code aiConfidenceScore} populated
     */
    @Transactional
    public Claim analyzeAndPersist(Claim claim, String sourceId, String excerpt, String title) {
        return analyzeAndPersistWithChunk(claim, sourceId, null, excerpt, title);
    }

    private Claim analyzeAndPersistWithChunk(Claim claim, String sourceId, Integer chunkId, String excerpt, String title) {
        // ── Phase 2: process/claim ─────────────────────────────────────────────
        log.info("Phase 2 – processing claim {} against sourceId={}", claim.getId(), sourceId);

        ClaimAnalysisRequest request = new ClaimAnalysisRequest(
                claim.getContent(),
                sourceId,
                excerpt,
                title
        );

        ClaimAnalysisResponse response;
        try {
            response = aiModelClient.processClaim(request);
        } catch (AiApiException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI process/claim call failed for claim " + claim.getId() + ": " + e.getMessage(), e);
        }

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI process/claim returned null for claim " + claim.getId());
        }

        // ── Validate AI response integrity ─────────────────────────────────────
        if (response.verdict() == null) {
            throw new AiValidationException(
                    "AI returned null verdict for claim " + claim.getId());
        }
        if (response.confidence() == null) {
            throw new AiValidationException(
                    "AI returned null confidence for claim " + claim.getId());
        }
        if (response.confidence().compareTo(BigDecimal.ZERO) < 0
                || response.confidence().compareTo(BigDecimal.ONE) > 0) {
            throw new AiValidationException(
                    "AI returned out-of-range confidence " + response.confidence()
                    + " for claim " + claim.getId() + ". Must be 0.0–1.0.");
        }

        log.info("Phase 2 complete – verdict={}, confidence={}", response.verdict(), response.confidence());

        // ── Persist: update Claim.aiConfidenceScore ────────────────────────────
        claim.setAiConfidenceScore(response.confidence());
        Claim updatedClaim = claimRepository.save(claim);

        // ── Find or fallback SourceChunk ───────────────────────────────────────
        SourceChunk sourceChunk = null;
        if (chunkId != null) {
            sourceChunk = sourceChunkRepository.findById(chunkId).orElse(null);
        }
        if (sourceChunk == null) {
            Integer srcIdVal = null;
            try {
                srcIdVal = Integer.valueOf(sourceId);
            } catch (NumberFormatException ignored) {}
            if (srcIdVal != null) {
                List<SourceChunk> chunks = sourceChunkRepository.findBySourceId(srcIdVal);
                // 1. Exact match
                for (SourceChunk c : chunks) {
                    if (c.getText().equals(excerpt)) {
                        sourceChunk = c;
                        break;
                    }
                }
                // 2. Substring/contains match
                if (sourceChunk == null) {
                    for (SourceChunk c : chunks) {
                        if (c.getText().contains(excerpt) || excerpt.contains(c.getText())) {
                            sourceChunk = c;
                            break;
                        }
                    }
                }
                // 3. Fallback to first chunk if still null
                if (sourceChunk == null && !chunks.isEmpty()) {
                    sourceChunk = chunks.get(0);
                }
            }
        }

        if (sourceChunk == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not find or match a valid source chunk for source ID: " + sourceId);
        }

        // ── Persist: Create EvidenceEdge ───────────────────────────────────────
        EvidenceEdge edge = new EvidenceEdge();
        edge.setClaim(updatedClaim);
        edge.setSourceChunk(sourceChunk);
        edge.setVerdict(response.verdict());
        edge.setConfidenceScore(response.confidence());
        edge.setExplanation(response.explanation());

        String missingEvidenceJson = null;
        if (response.missingEvidence() != null) {
            try {
                missingEvidenceJson = objectMapper.writeValueAsString(response.missingEvidence());
            } catch (Exception e) {
                log.error("Failed to serialize missing evidence: {}", response.missingEvidence(), e);
            }
        }
        edge.setMissingEvidence(missingEvidenceJson);

        evidenceEdgeRepository.save(edge);

        log.info("Persisted evidence edge for claim {}", claim.getId());
        return updatedClaim;
    }
}
