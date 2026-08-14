-- ==========================================
-- OPS: track when a job actually started processing (for the sweeper)
-- ==========================================
ALTER TABLE ai_evaluation_jobs
    ADD COLUMN started_at DATETIME NULL AFTER status;

-- ==========================================
-- TELEMETRY: server-computed time from review round to student action
-- ==========================================
ALTER TABLE evidence_revision_traces
    ADD COLUMN round_duration_ms BIGINT NULL AFTER after_section_version;
