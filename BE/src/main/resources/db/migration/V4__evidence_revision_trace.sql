-- ==========================================
-- 16. EVIDENCE REVISION TRACE (Option B)
-- One row per citation-review run + one row per finding (identity anchor).
-- ==========================================

CREATE TABLE citation_review_rounds (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    section_id BINARY(16) NOT NULL,
    section_version INT NOT NULL,
    requested_by BINARY(16) NOT NULL,
    content_fingerprint VARCHAR(64) NOT NULL,
    style VARCHAR(64) NOT NULL,
    generation_meta JSON NULL,
    summary TEXT NULL,
    complete BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_citation_review_rounds_section_fp (section_id, content_fingerprint),
    INDEX idx_citation_review_rounds_section (section_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE CASCADE,
    FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE evidence_revision_traces (
    id BINARY(16) NOT NULL PRIMARY KEY,
    round_id BINARY(16) NOT NULL,
    finding_index INT NOT NULL,
    section_id BINARY(16) NOT NULL,
    suggested_action VARCHAR(40) NOT NULL,
    criticality VARCHAR(40) NULL,
    parent_header VARCHAR(255) NULL,
    excerpt TEXT NOT NULL,
    excerpt_start INT NOT NULL,
    excerpt_end INT NOT NULL,
    rationale TEXT NOT NULL,
    confidence DECIMAL(5,4) NULL,
    source_id BINARY(16) NULL,
    chunk_id BINARY(16) NULL,
    evidence_quote TEXT NULL,
    evidence_relation VARCHAR(40) NULL,
    student_action VARCHAR(40) NULL,
    explanation TEXT NULL,
    after_passage LONGTEXT NULL,
    after_fingerprint VARCHAR(64) NULL,
    after_section_version INT NULL,
    source_replaced BOOLEAN NULL,
    outcome VARCHAR(20) NULL,
    instructor_id BINARY(16) NULL,
    judgment VARCHAR(20) NULL,
    instructor_feedback TEXT NULL,
    judged_at DATETIME NULL,
    linked_round_id BINARY(16) NULL,
    linked_mode VARCHAR(30) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_evidence_revision_traces_round_finding (round_id, finding_index),
    INDEX idx_evidence_revision_traces_section (section_id),
    FOREIGN KEY (round_id) REFERENCES citation_review_rounds(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES documents(id) ON DELETE SET NULL,
    FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE SET NULL,
    FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (linked_round_id) REFERENCES citation_review_rounds(id) ON DELETE SET NULL,
    CONSTRAINT chk_evidence_revision_traces_student_action
        CHECK (student_action IS NULL OR student_action IN (
            'ADD_CITATION', 'PARAPHRASE', 'QUALIFY', 'SYNTHESIZE', 'QUOTE', 'REMOVE', 'DISMISS_WITH_REASON'
        )),
    CONSTRAINT chk_evidence_revision_traces_outcome
        CHECK (outcome IS NULL OR outcome IN (
            'RESOLVED', 'PARTIALLY_RESOLVED', 'UNRESOLVED', 'STALE'
        )),
    CONSTRAINT chk_evidence_revision_traces_judgment
        CHECK (judgment IS NULL OR judgment IN ('EFFECTIVE', 'PARTIAL', 'INEFFECTIVE')),
    CONSTRAINT chk_evidence_revision_traces_linked_mode
        CHECK (linked_mode IS NULL OR linked_mode IN (
            'VERBATIM_CONTINUATION', 'REVISION_CHAIN'
        ))
);
