package com.evidencepilot.controller;

import com.evidencepilot.service.AiModelClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "Backend and AI worker health checks")
public class HealthController {

    private final AiModelClient aiModelClient;

    @Operation(summary = "Check backend and AI worker health")
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "ai", aiModelClient.health()
        );
    }
}
