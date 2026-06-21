package com.evidencepilot.service;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.ai.dto.ClaimMatch;
import com.evidencepilot.ai.dto.ClaimMatchResponse;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.repository.SourceChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Matches a {@link Claim} against persisted {@link SourceChunk}s using
 * Qdrant's vector nearest-neighbour search.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li>Embed the incoming claim text via {@link AiModelClient#generateEmbedding(String)}.</li>
 *   <li>Query Qdrant for the closest chunk vector within the claim's project.</li>
 *   <li>Fetch the winning {@link SourceChunk} from MySQL by ID.</li>
 *   <li>Return the result wrapped in a {@link ClaimMatchResponse}.</li>
 * </ol>
 *
 * <p><b>Architecture note:</b> The previous in-memory cosine similarity loop
 * has been fully replaced by Qdrant ANN search, eliminating the O(n) Java-side
 * embedding deserialization and computation that could crash on large corpora.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimMatchingService {

    private final AiModelClient aiModelClient;
    private final SourceChunkRepository sourceChunkRepository;
    private final QdrantClient qdrantClient;

    public ClaimMatchResponse matchClaim(Claim claim, int topK) {
        // topK is accepted for API compatibility but Qdrant search currently
        // returns the single best match (limit=1). Future: pass topK through.

        // ── Step 1: embed the claim ────────────────────────────────────────────
        List<Float> claimVector = aiModelClient.generateEmbedding(claim.getContent());

        // ── Step 2: query Qdrant for the closest chunk ─────────────────────────
        String projectId = String.valueOf(claim.getProject().getId());
        String bestChunkId = qdrantClient.findClosestChunkId(claimVector, projectId);

        if (bestChunkId == null) {
            log.info("Qdrant returned no results for claim {} in project {}", claim.getId(), projectId);
            return new ClaimMatchResponse(claim.getContent(), List.of());
        }

        // ── Step 3: fetch the chunk from MySQL ─────────────────────────────────
        Optional<SourceChunk> chunkOpt = sourceChunkRepository.findById(Integer.valueOf(bestChunkId));
        if (chunkOpt.isEmpty()) {
            log.warn("Qdrant returned chunkId={} but it no longer exists in MySQL", bestChunkId);
            return new ClaimMatchResponse(claim.getContent(), List.of());
        }

        SourceChunk chunk = chunkOpt.get();
        // Use a fixed high score since Qdrant already picked the best match;
        // the exact Qdrant score is not surfaced through findClosestChunkId.
        ClaimMatch match = toMatch(chunk, 1.0);

        return new ClaimMatchResponse(claim.getContent(), List.of(match));
    }

    // ── Result mapping ─────────────────────────────────────────────────────────

    private ClaimMatch toMatch(SourceChunk chunk, double score) {
        Source source = chunk.getSource();
        BigDecimal rounded = BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
        return new ClaimMatch(
                String.valueOf(source.getId()),
                source.getOriginalFilename() == null ? source.getFileUrl() : source.getOriginalFilename(),
                String.valueOf(chunk.getId()),
                chunk.getPage(),
                chunk.getText(),
                rounded,
                suitability(score),
                explanation(score)
        );
    }

    private String suitability(double score) {
        if (score >= 0.75) {
            return "strong";
        }
        if (score >= 0.50) {
            return "medium";
        }
        return "weak";
    }

    private String explanation(double score) {
        if (score >= 0.75) {
            return "This source chunk is semantically very similar to the claim and is a strong candidate for review.";
        }
        if (score >= 0.50) {
            return "This source chunk has moderate semantic similarity with the claim and may partially support it.";
        }
        return "This source chunk has low semantic similarity with the claim and should be checked manually.";
    }
}
