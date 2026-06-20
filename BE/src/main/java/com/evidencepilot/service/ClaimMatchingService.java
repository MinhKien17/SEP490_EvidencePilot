package com.evidencepilot.service;

import com.evidencepilot.ai.dto.ClaimMatch;
import com.evidencepilot.ai.dto.ClaimMatchResponse;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.repository.SourceChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimMatchingService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9-]+");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "for", "in", "is", "it", "of", "on", "or", "the", "to", "with"
    );

    private final SourceChunkRepository sourceChunkRepository;

    public ClaimMatchResponse matchClaim(Claim claim, int topK) {
        int safeTopK = Math.max(1, Math.min(topK, 10));
        Map<String, Long> claimTerms = tokens(claim.getContent());
        List<SourceChunk> chunks = sourceChunkRepository
                .findBySourceProjectIdAndSourceActiveTrueAndActiveTrueOrderBySourceIdAscChunkIndexAsc(claim.getProject().getId());

        List<ScoredChunk> scored = new ArrayList<>();
        for (SourceChunk chunk : chunks) {
            double score = score(claimTerms, tokens(chunk.getText()));
            if (score > 0) {
                scored.add(new ScoredChunk(score, chunk));
            }
        }

        List<ClaimMatch> matches = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(safeTopK)
                .map(item -> toMatch(item.chunk(), item.score()))
                .toList();

        return new ClaimMatchResponse(claim.getContent(), matches);
    }

    private Map<String, Long> tokens(String text) {
        return TOKEN_PATTERN.matcher(text.toLowerCase())
                .results()
                .map(MatchResult::group)
                .filter(word -> word.length() > 2)
                .filter(word -> !STOPWORDS.contains(word))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private double score(Map<String, Long> claimTerms, Map<String, Long> chunkTerms) {
        if (claimTerms.isEmpty() || chunkTerms.isEmpty()) {
            return 0.0;
        }
        long weightedOverlap = claimTerms.entrySet().stream()
                .filter(entry -> chunkTerms.containsKey(entry.getKey()))
                .mapToLong(entry -> Math.min(entry.getValue(), chunkTerms.get(entry.getKey())))
                .sum();
        if (weightedOverlap == 0) {
            return 0.0;
        }
        double denominator = Math.sqrt(sumCounts(claimTerms)) * Math.sqrt(sumCounts(chunkTerms));
        return Math.min(weightedOverlap / denominator, 1.0);
    }

    private long sumCounts(Map<String, Long> terms) {
        return terms.values().stream().mapToLong(Long::longValue).sum();
    }

    private ClaimMatch toMatch(SourceChunk chunk, double score) {
        Source source = chunk.getSource();
        BigDecimal rounded = BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
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
        if (score >= 0.45) {
            return "strong";
        }
        if (score >= 0.25) {
            return "medium";
        }
        return "weak";
    }

    private String explanation(double score) {
        if (score >= 0.45) {
            return "This source chunk shares strong terminology with the claim and is a good candidate for review.";
        }
        if (score >= 0.25) {
            return "This source chunk has partial overlap with the claim and may support part of it.";
        }
        return "This source chunk has limited overlap with the claim and should be checked manually.";
    }

    private record ScoredChunk(double score, SourceChunk chunk) {
    }
}
