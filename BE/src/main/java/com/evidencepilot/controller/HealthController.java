package com.evidencepilot.controller;

import com.evidencepilot.service.HealthService;
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

    private final HealthService healthService;

    @Operation(summary = "Check backend liveness")
    @GetMapping("/api/health/live")
    public Map<String, Object> live() {
        return healthService.checkLiveness();
    }

    @Operation(summary = "Check backend readiness with component details")
    @GetMapping("/api/health/ready")
    public Map<String, Object> ready() {
        return healthService.checkReadiness();
    }

    @Operation(summary = "Check backend and AI worker health (legacy)")
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return healthService.checkReadiness();
    }
}
