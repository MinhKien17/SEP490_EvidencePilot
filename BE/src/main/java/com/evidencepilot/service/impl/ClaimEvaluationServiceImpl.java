package com.evidencepilot.service.impl;

import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.dto.request.ClaimRequest;
import com.evidencepilot.dto.response.ClaimEvaluationResponse;
import com.evidencepilot.service.ClaimEvaluationService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.OllamaGateway;
import com.evidencepilot.service.QdrantGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEvaluationServiceImpl implements ClaimEvaluationService {

    private static final int TOP_K = 20;
    private static final int MAX_PROMPT_LENGTH = 12000;

    private final QdrantGateway qdrantGateway;
    private final OllamaGateway ollamaGateway;
    private final SparseVectorGenerator sparseVectorGenerator;
    private final DocumentService documentService;

    public ClaimEvaluationResponse evaluate(UUID documentId, ClaimRequest request) {
        documentService.getDocumentById(documentId);
        log.info("Evaluating claim for document {}: {}", documentId, request.claimText());

        List<Float> denseVector = ollamaGateway.getDenseEmbedding(request.claimText());
        SparseVector sparseVector = sparseVectorGenerator.generate(request.claimText());
        List<String> context = qdrantGateway.searchDocumentContext(documentId, denseVector, sparseVector, TOP_K);

        if (context.isEmpty()) {
            log.warn("No context found for claim in document {}", documentId);
            return new ClaimEvaluationResponse(
                    request.claimText(),
                    "No relevant context found to evaluate this claim.",
                    List.of()
            );
        }

        String prompt = buildEvaluationPrompt(context, request.claimText());
        String evaluation = ollamaGateway.generateEvaluation(prompt);

        log.info("Evaluation complete for document {}", documentId);
        return new ClaimEvaluationResponse(request.claimText(), evaluation, context);
    }

    private String buildEvaluationPrompt(List<String> context, String claimText) {
        String template = """
                You are a strict academic evaluator. Based ONLY on the provided context, evaluate the following claim.
                Return exactly one of: [Supported], [Refuted], or [Not Addressed], followed by a brief explanation.

                Context:
                %s

                Claim: %s

                Evaluation:""";
        String contextStr = String.join("\n\n", context);
        String prompt = template.formatted(contextStr, claimText);
        if (prompt.length() <= MAX_PROMPT_LENGTH) {
            return prompt;
        }
        int overhead = template.formatted("", claimText).length();
        int maxContextLen = MAX_PROMPT_LENGTH - overhead;
        if (maxContextLen <= 0) {
            log.warn("Claim text alone exceeds prompt limit, sending without context");
            return template.formatted("", claimText);
        }
        String truncated = contextStr.substring(0, Math.min(contextStr.length(), maxContextLen));
        int lastBreak = truncated.lastIndexOf('\n');
        if (lastBreak > maxContextLen / 2) {
            truncated = truncated.substring(0, lastBreak);
        }
        return template.formatted(truncated, claimText);
    }
}
