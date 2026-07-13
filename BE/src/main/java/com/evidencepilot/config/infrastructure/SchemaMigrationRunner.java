package com.evidencepilot.config.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class SchemaMigrationRunner implements CommandLineRunner {

    private static final String EXPECTED_CONSTRAINT =
            "processing_status IN ('PENDING_UPLOAD','UPLOADED','METADATA_FETCHED','PDF_DOWNLOADED','QUEUED','PROCESSING','RAW_EXTRACTED','READY','COMPLETED','FAILED')";

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            String constraintName = findCheckConstraint();
            if (constraintName != null) {
                jdbcTemplate.execute("ALTER TABLE documents DROP CHECK " + constraintName);
                log.info("Dropped old CHECK constraint: {}", constraintName);
            }
            jdbcTemplate.execute("ALTER TABLE documents ADD CONSTRAINT documents_processing_status_check CHECK (" + EXPECTED_CONSTRAINT + ")");
            log.info("Applied processing_status CHECK constraint");
        } catch (Exception e) {
            log.warn("Could not migrate CHECK constraint (non-fatal): {}", e.getMessage());
        }
    }

    private String findCheckConstraint() {
        try {
            var results = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                    "WHERE TABLE_NAME = 'documents' AND CONSTRAINT_TYPE = 'CHECK'");
            if (!results.isEmpty()) {
                return (String) results.get(0).get("CONSTRAINT_NAME");
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
