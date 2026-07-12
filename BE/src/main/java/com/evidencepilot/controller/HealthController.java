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
        Map<String, Object> ai;
        try {
            ai = aiModelClient.health();
        } catch (RuntimeException exception) {
            ai = Map.of("status", "unavailable");
        }
        return Map.of("status", "ok", "ai", ai);
    }
}
