-- ==========================================
-- OPS: track when a job actually started processing (for the sweeper)
-- ==========================================
ALTER TABLE ai_evaluation_jobs
    ADD COLUMN started_at DATETIME NULL AFTER status;
CREATE INDEX idx_ai_eval_started ON ai_evaluation_jobs (status, started_at);

-- ==========================================
-- TELEMETRY: HITL fidelity columns on evidence revision traces
-- ==========================================
ALTER TABLE evidence_revision_traces
    ADD COLUMN actual_edit_hash VARCHAR(64) NULL AFTER after_fingerprint,
    ADD COLUMN accepted TINYINT(1) NULL AFTER actual_edit_hash,
    ADD COLUMN round_duration_ms BIGINT NULL AFTER accepted;