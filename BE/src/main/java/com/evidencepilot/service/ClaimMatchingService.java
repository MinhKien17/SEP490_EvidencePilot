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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimMatchingService {

    private static final BigDecimal STRONG_THRESHOLD = new BigDecimal("0.75");
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("0.50");

    private final AiModelClient aiModelClient;
    private final SourceChunkRepository sourceChunkRepository;
    private final QdrantClient qdrantClient;

    public ClaimMatchResponse matchClaim(Claim claim, int topK) {
        int safeTopK = Math.max(1, Math.min(topK, 20));
        List<Float> claimVector = aiModelClient.generateEmbedding(claim.getContent());
        String projectId = String.valueOf(claim.getProject().getId());
        List<QdrantSearchResult> results = qdrantClient.findClosestChunks(
                claimVector,
                "PROJECT",
                projectId,
                safeTopK
        );

        if (results.isEmpty()) {
            log.info("Qdrant returned no results for claim {} in project {}", claim.getId(), projectId);
            return new ClaimMatchResponse(claim.getContent(), List.of());
        }

        List<ClaimMatch> matches = new ArrayList<>();
        for (QdrantSearchResult result : results) {
            Integer chunkId = parseInteger(result.chunkId());
            if (chunkId == null) {
                log.warn("Qdrant returned non-numeric chunkId={}", result.chunkId());
                continue;
            }
            Optional<SourceChunk> chunk = sourceChunkRepository.findById(chunkId);
            if (chunk.isEmpty()) {
                log.warn("Qdrant returned chunkId={} but it no longer exists in MySQL", result.chunkId());
                continue;
            }
            matches.add(toMatch(chunk.get(), result.score()));
        }

        return new ClaimMatchResponse(claim.getContent(), matches);
    }

    private ClaimMatch toMatch(SourceChunk chunk, BigDecimal score) {
        Source source = chunk.getSource();
        BigDecimal rounded = score.setScale(4, RoundingMode.HALF_UP);
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

    private String suitability(BigDecimal score) {
        if (score.compareTo(STRONG_THRESHOLD) >= 0) {
            return "strong";
        }
        if (score.compareTo(MEDIUM_THRESHOLD) >= 0) {
            return "medium";
        }
        return "weak";
    }

    private String explanation(BigDecimal score) {
        if (score.compareTo(STRONG_THRESHOLD) >= 0) {
            return "This source chunk is semantically very similar to the claim and is a strong candidate for review.";
        }
        if (score.compareTo(MEDIUM_THRESHOLD) >= 0) {
            return "This source chunk has moderate semantic similarity with the claim and may partially support it.";
        }
        return "This source chunk has low semantic similarity with the claim and should be checked manually.";
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
