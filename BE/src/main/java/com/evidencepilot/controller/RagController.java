package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ClaimRequest;
import com.evidencepilot.dto.response.ClaimEvaluationResponse;
import com.evidencepilot.service.OllamaGateway;
import com.evidencepilot.service.QdrantGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/paper/{documentId}/claims", "/api/sources/{documentId}/claims"})
public class RagController {

    private final QdrantGateway qdrantGateway;
    private final OllamaGateway ollamaGateway;

    @PostMapping("/match")
    public ClaimEvaluationResponse matchClaim(
            @PathVariable UUID documentId,
            @RequestBody ClaimRequest request
    ) {
        log.info("RAG match requested for document {}: {}", documentId, request.claimText());

        List<Float> vector = ollamaGateway.getEmbedding(request.claimText());
        List<String> context = qdrantGateway.searchDocumentContext(documentId, vector, 3);

        if (context.isEmpty()) {
            log.warn("No context found for claim in document {}", documentId);
            return new ClaimEvaluationResponse(
                    request.claimText(),
                    "No relevant context found to evaluate this claim.",
                    List.of()
            );
        }

        String contextStr = String.join("\n\n", context);
        String prompt = String.format("""
                You are a strict academic evaluator. Based ONLY on the provided context, evaluate the following claim.
                Return exactly one of: [Supported], [Refuted], or [Not Addressed], followed by a brief explanation.

                Context:
                %s

                Claim: %s

                Evaluation:""", contextStr, request.claimText());

        String evaluation = ollamaGateway.generateEvaluation(prompt);

        log.info("RAG evaluation complete for document {}", documentId);
        return new ClaimEvaluationResponse(request.claimText(), evaluation, context);
    }
}
