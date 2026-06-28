package com.evidencepilot.service.impl;

import com.evidencepilot.dto.SparseVector;
import com.evidencepilot.dto.request.ClaimRequest;
import com.evidencepilot.dto.response.ClaimEvaluationResponse;
import com.evidencepilot.service.ClaimEvaluationService;
import com.evidencepilot.service.OllamaGateway;
import com.evidencepilot.service.QdrantGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEvaluationServiceImpl implements ClaimEvaluationService {

    private static final int TOP_K = 20;

    private final QdrantGateway qdrantGateway;
    private final OllamaGateway ollamaGateway;

    public ClaimEvaluationResponse evaluate(UUID documentId, ClaimRequest request) {
        log.info("Evaluating claim for document {}: {}", documentId, request.claimText());

        List<Float> denseVector = ollamaGateway.getEmbedding(request.claimText());
        SparseVector sparseVector = ollamaGateway.getSparseEmbedding(request.claimText());
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
        String contextStr = String.join("\n\n", context);
        return String.format("""
                You are a strict academic evaluator. Based ONLY on the provided context, evaluate the following claim.
                Return exactly one of: [Supported], [Refuted], or [Not Addressed], followed by a brief explanation.

                Context:
                %s

                Claim: %s

                Evaluation:""", contextStr, claimText);
    }
}
