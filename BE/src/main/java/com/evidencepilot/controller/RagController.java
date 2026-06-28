package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ClaimRequest;
import com.evidencepilot.dto.response.ClaimEvaluationResponse;
import com.evidencepilot.service.ClaimEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/paper/{documentId}/claims", "/api/sources/{documentId}/claims"})
public class RagController {

    private final ClaimEvaluationService claimEvaluationService;

    @PostMapping("/match")
    public ClaimEvaluationResponse matchClaim(
            @PathVariable UUID documentId,
            @RequestBody ClaimRequest request
    ) {
        return claimEvaluationService.evaluate(documentId, request);
    }
}
