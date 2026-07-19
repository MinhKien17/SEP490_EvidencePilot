package com.evidencepilot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final DataSource dataSource;
    private final AiModelClient aiModelClient;

    public Map<String, Object> checkLiveness() {
        return Map.of("status", "UP");
    }

    public Map<String, Object> checkReadiness() {
        Map<String, Object> components = new LinkedHashMap<>();
        String overall = "UP";

        try {
            long start = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                long latency = System.currentTimeMillis() - start;
                components.put("database", Map.of(
                        "status", "UP",
                        "latencyMs", latency,
                        "product", meta.getDatabaseProductName()));
            }
        } catch (Exception e) {
            components.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
            overall = "DOWN";
        }

        try {
            Map<String, Object> aiHealth = aiModelClient.health();
            components.put("aiWorker", Map.of("status", "UP", "details", aiHealth));
        } catch (Exception e) {
            components.put("aiWorker", Map.of("status", "DOWN", "error", e.getMessage()));
            if (!"DOWN".equals(overall)) overall = "DEGRADED";
        }

        return Map.of("status", overall, "components", components);
    }
}
