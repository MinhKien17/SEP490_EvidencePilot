ALTER TABLE ai_evaluation_jobs
    ADD COLUMN progress_current INT NOT NULL DEFAULT 0 AFTER started_at,
    ADD COLUMN progress_total INT NOT NULL DEFAULT 0 AFTER progress_current;
