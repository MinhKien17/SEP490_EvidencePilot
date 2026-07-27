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
            ensureColumnIsVarchar();
            String constraintName = findCheckConstraint();
            if (constraintName != null) {
                jdbcTemplate.execute("ALTER TABLE documents DROP CHECK " + constraintName);
                log.info("Dropped old CHECK constraint: {}", constraintName);
            }
            jdbcTemplate.execute("ALTER TABLE documents ADD CONSTRAINT documents_processing_status_check CHECK (" + EXPECTED_CONSTRAINT + ")");
            log.info("Applied processing_status CHECK constraint");
        } catch (Exception e) {
            log.warn("Could not migrate processing_status column (non-fatal): {}", e.getMessage());
        }
    }

    private void ensureColumnIsVarchar() {
        String columnType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'documents' AND COLUMN_NAME = 'processing_status'",
                String.class);
        if (columnType != null && columnType.equalsIgnoreCase("enum")) {
            jdbcTemplate.execute(
                    "ALTER TABLE documents MODIFY COLUMN processing_status VARCHAR(50) NOT NULL");
            log.info("Changed processing_status column from ENUM to VARCHAR(50)");
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
