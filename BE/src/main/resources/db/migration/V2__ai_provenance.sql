ALTER TABLE ai_suggestions
    ADD COLUMN model_name       VARCHAR(100) NOT NULL DEFAULT 'unknown',
    ADD COLUMN model_version    VARCHAR(50)  NULL,
    ADD COLUMN prompt_version   VARCHAR(50)  NULL,
    ADD COLUMN rubric_version   VARCHAR(50)  NULL,
    ADD COLUMN evaluated_at     DATETIME     NULL,
    ADD COLUMN score_breakdown  JSON         NULL;
