ALTER TABLE feedback_requests
    ADD COLUMN section_validation TEXT NULL;

ALTER TABLE paper_sections
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
