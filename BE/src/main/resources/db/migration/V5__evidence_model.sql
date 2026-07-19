ALTER TABLE ai_suggestions
    ADD COLUMN relation       VARCHAR(20) NULL,
    ADD COLUMN strength_score INT         NULL,
    ADD COLUMN strength_band  VARCHAR(10) NULL;

ALTER TABLE claim_evidence_mappings
    ADD COLUMN relation            VARCHAR(20) NULL,
    ADD COLUMN strength_score      INT         NULL,
    ADD COLUMN strength_band       VARCHAR(10) NULL,
    ADD COLUMN review_status       VARCHAR(20) NULL,
    ADD COLUMN reviewed_by         BINARY(16)  NULL REFERENCES users(id),
    ADD COLUMN reviewed_at         DATETIME    NULL,
    ADD COLUMN review_note         TEXT        NULL,
    ADD COLUMN relation_override   VARCHAR(20) NULL,
    ADD COLUMN score_breakdown     JSON        NULL;
