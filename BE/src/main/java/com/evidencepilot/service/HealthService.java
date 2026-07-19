package com.evidencepilot.service;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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
    private final QdrantClient qdrantClient;
    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

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

        try {
            Map<String, Object> qdrantHealth = qdrantClient.health();
            String qdrantStatus = (String) qdrantHealth.get("status");
            components.put("qdrant", qdrantHealth);
            if (!"UP".equals(qdrantStatus)) {
                overall = "DOWN";
            }
        } catch (Exception e) {
            components.put("qdrant", Map.of("status", "DOWN", "error", e.getMessage()));
            overall = "DOWN";
        }

        try {
            long start = System.currentTimeMillis();
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            long latency = System.currentTimeMillis() - start;
            components.put("minio", Map.of("status", "UP", "latencyMs", latency));
        } catch (Exception e) {
            components.put("minio", Map.of("status", "DOWN", "error", e.getMessage()));
            overall = "DOWN";
        }

        try {
            long start = System.currentTimeMillis();
            rabbitTemplate.execute(channel -> {
                channel.queueDeclarePassive("extraction.queue");
                return true;
            });
            long latency = System.currentTimeMillis() - start;
            components.put("rabbitmq", Map.of("status", "UP", "latencyMs", latency));
        } catch (Exception e) {
            components.put("rabbitmq", Map.of("status", "DOWN", "error", e.getMessage()));
            if (!"DOWN".equals(overall)) overall = "DEGRADED";
        }

        return Map.of("status", overall, "components", components);
    }
}
