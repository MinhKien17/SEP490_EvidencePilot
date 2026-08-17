-- ==========================================
-- 1. CORE IDENTITY & ACCESS
-- ==========================================
CREATE TABLE users (
    id BINARY(16) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMIN')),
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (account_status IN ('PENDING', 'ACTIVE', 'BANNED', 'DELETED')),
    student_code VARCHAR(50) NULL UNIQUE,
    password_change_notice_pending BOOLEAN NOT NULL DEFAULT FALSE,
    password_reset_token_hash VARCHAR(255) UNIQUE,
    password_reset_token_expires_at DATETIME,
    password_reset_requested_at DATETIME,
    token_version INT NOT NULL DEFAULT 0,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 2. PROJECT WORKSPACE & COLLABORATION
-- ==========================================
CREATE TABLE projects (
    id BINARY(16) NOT NULL PRIMARY KEY,
    opt_version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL CHECK (status IN ('CREATED', 'ASSIGNED', 'IN_PROGRESS', 'SUBMITTED_FOR_REVIEW', 'RETURNED', 'APPROVED', 'ARCHIVED')),
    target_standard VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_projects_target_standard CHECK (target_standard IS NULL OR target_standard IN ('IEEE', 'ACM', 'SPRINGER_LNCS', 'APA', 'MLA', 'CUSTOM'))
);

CREATE TABLE project_members (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('LEADER', 'MEMBER', 'INSTRUCTOR')),
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_project_members_unique (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- 3. COLLECTIONS & DOCUMENTS
-- ==========================================
CREATE TABLE collection_categories (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collections (
    id BINARY(16) NOT NULL PRIMARY KEY,
    instructor_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BINARY(16),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES collection_categories(id) ON DELETE SET NULL
);

CREATE TABLE documents (
    id BINARY(16) NOT NULL PRIMARY KEY,
    opt_version BIGINT NOT NULL DEFAULT 0,
    project_id BINARY(16),
    collection_id BINARY(16),
    uploaded_by BINARY(16) NOT NULL,
    doc_type VARCHAR(50) NOT NULL CHECK (doc_type IN ('PAPER', 'SOURCE')),
    file_url VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(255),
    file_size_bytes BIGINT,
    file_hash_sha256 VARCHAR(64),
    processing_status VARCHAR(50) NOT NULL CHECK (processing_status IN ('PENDING_UPLOAD', 'UPLOADED', 'METADATA_FETCHED', 'PDF_DOWNLOADED', 'QUEUED', 'PROCESSING', 'RAW_EXTRACTED', 'PARTIAL', 'READY', 'COMPLETED', 'FAILED')),
    processing_error TEXT,
    chunk_count INT DEFAULT 0,
    processed_at DATETIME,
    published_at DATETIME,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    doi VARCHAR(255),
    title VARCHAR(500),
    authors TEXT,
    publication_year INT,
    publisher VARCHAR(255),
    openalex_topic VARCHAR(255),
    openalex_subfield VARCHAR(255),
    openalex_field VARCHAR(255),
    openalex_domain VARCHAR(255),
    cited_by_count INT,
    extraction_quality JSON,
    download_token VARCHAR(36) NOT NULL,
    preamble_tex LONGTEXT NULL,
    front_matter_tex LONGTEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_documents_project_id (project_id),
    INDEX idx_documents_collection_id (collection_id),
    INDEX idx_documents_file_hash_sha256 (file_hash_sha256),
    INDEX idx_documents_processing_status (processing_status),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE SET NULL,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- 4. EVIDENCE EXTRACTION (AI PIPELINE)
-- ==========================================
CREATE TABLE document_texts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    document_id BINARY(16) NOT NULL UNIQUE,
    extracted_text LONGTEXT NOT NULL,
    extraction_method VARCHAR(50) NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE TABLE document_chunks (
    id BINARY(16) NOT NULL PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    chunk_index INT NOT NULL,
    `text` TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_document_chunks_document_index UNIQUE (document_id, chunk_index),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE TABLE document_references (
    id BINARY(16) NOT NULL PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    reference_index INT NOT NULL,
    raw_text TEXT NOT NULL,
    title VARCHAR(255),
    publication_year INT,
    cited_by_count INT,
    doi VARCHAR(255),
    edge_type VARCHAR(50) NOT NULL,
    CONSTRAINT uq_document_references_order UNIQUE (document_id, edge_type, reference_index),
    CONSTRAINT chk_document_references_edge_type CHECK (edge_type IN ('REFERENCES', 'CITED_BY')),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- ==========================================
-- 5. THE PAPER STRUCTURE (OVERLEAF MODEL)
-- ==========================================
CREATE TABLE paper_sections (
    id BINARY(16) NOT NULL PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    assigned_user_id BINARY(16),
    section_order INT NOT NULL,
    section_title VARCHAR(255) NOT NULL,
    content_tex LONGTEXT NOT NULL,
    previous_content_tex LONGTEXT,
    version INT DEFAULT 1,
    opt_version BIGINT NOT NULL DEFAULT 0,
    content_md_cache LONGTEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_paper_sections (document_id, section_order),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE project_media (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    uploaded_by BINARY(16) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    tex_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_media_storage UNIQUE (project_id, storage_key),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- 7. SHARED COLLECTIONS & DOCUMENTS
-- ==========================================
CREATE TABLE project_collections (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    collection_id BINARY(16) NOT NULL,
    linked_by BINARY(16) NOT NULL,
    linked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_project_collections_unique (project_id, collection_id),
    INDEX idx_project_collections_collection (collection_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE,
    FOREIGN KEY (linked_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE project_documents (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    document_id BINARY(16) NOT NULL,
    project_collection_id BINARY(16),
    pinned BOOLEAN NOT NULL DEFAULT TRUE,
    shared_by BINARY(16) NOT NULL,
    shared_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_project_documents_unique (project_id, document_id),
    INDEX idx_project_documents_collection_link (project_collection_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (project_collection_id) REFERENCES project_collections(id) ON DELETE SET NULL,
    FOREIGN KEY (shared_by) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- 8. FEEDBACK & ASYNC STATE
-- ==========================================
CREATE TABLE feedback_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    instructor_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RETURNED', 'REVIEWED', 'REJECTED')),
    section_validation TEXT,
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE instructor_feedbacks (
    id BINARY(16) NOT NULL PRIMARY KEY,
    request_id BINARY(16) NOT NULL,
    section_id BINARY(16) NOT NULL,
    instructor_id BINARY(16) NOT NULL,
    line_reference VARCHAR(100),
    content TEXT NOT NULL,
    answered BOOLEAN NOT NULL DEFAULT FALSE,
    answer_content TEXT,
    answered_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    section_version INT,
    updated_at DATETIME,
    updated_by BINARY(16),
    FOREIGN KEY (request_id) REFERENCES feedback_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- 9. SYSTEM NOTIFICATIONS
-- ==========================================
CREATE TABLE system_notifications (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    actor_id BINARY(16),
    action_type VARCHAR(50) NOT NULL,
    entity_id BINARY(16),
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ==========================================
-- 10. EXPORT JOBS
-- ==========================================
CREATE TABLE export_jobs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    format VARCHAR(20) NOT NULL DEFAULT 'TEX',
    download_url VARCHAR(1024),
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_export_jobs_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_export_jobs_format CHECK (format IN ('TEX', 'TRACEABILITY')),
    INDEX idx_export_project (project_id),
    INDEX idx_export_user (user_id),
    INDEX idx_export_status (status),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- 11. AUDIT TRAIL
-- ==========================================
CREATE TABLE audit_logs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    actor_id BINARY(16) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BINARY(16),
    old_value JSON,
    new_value JSON,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_actor (actor_id),
    INDEX idx_audit_occurred (occurred_at),
    FOREIGN KEY (actor_id) REFERENCES users(id)
);

-- ==========================================
-- 12. PROJECT CHECKPOINTS
-- ==========================================
CREATE TABLE project_checkpoints (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    snapshot_json LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_checkpoint_project (project_id, created_at),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- ==========================================
-- 13. AI REVIEW SNAPSHOTS (result cache)
-- ==========================================
CREATE TABLE review_snapshots (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    style VARCHAR(50) NOT NULL,
    input_fingerprint VARCHAR(64) NOT NULL,
    response_json LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_snapshots_lookup UNIQUE (project_id, style, input_fingerprint),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- ==========================================
-- 14. AI EVALUATION JOBS (async section-audit / paper-review evaluation)
-- ==========================================
CREATE TABLE ai_evaluation_jobs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    kind VARCHAR(50) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at DATETIME NULL,
    result_json LONGTEXT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    CONSTRAINT chk_ai_evaluation_jobs_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_ai_evaluation_jobs_kind CHECK (kind IN ('SECTION_CITATION_REVIEW', 'SECTION_SUGGESTION', 'SOURCE_MATCHES', 'TRACE_RECHECK')),
    INDEX idx_ai_eval_project (project_id),
    INDEX idx_ai_eval_status (status),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- ==========================================
-- 15. REVIEW GUIDES (read-only per-section-type reference)
-- ==========================================
CREATE TABLE section_review_guides (
    section_type VARCHAR(100) NOT NULL PRIMARY KEY,
    guidance TEXT NOT NULL,
    checklist_json JSON,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ==========================================
-- 16. EVIDENCE REVISION TRACE
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
    INDEX idx_citation_review_rounds_section_fp (section_id, content_fingerprint),
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
    round_duration_ms BIGINT NULL,
    source_replaced BOOLEAN NULL,
    outcome VARCHAR(20) NULL,
    instructor_id BINARY(16) NULL,
    judgment VARCHAR(20) NULL,
    instructor_feedback TEXT NULL,
    judged_at DATETIME NULL,
    linked_round_id BINARY(16) NULL,
    linked_mode VARCHAR(30) NULL,
    ai_recheck_judgment VARCHAR(20) NULL,
    ai_recheck_reason TEXT NULL,
    ai_rechecked_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_evidence_revision_traces_round_finding (round_id, finding_index),
    INDEX idx_evidence_revision_traces_section (section_id),
    CONSTRAINT chk_evidence_revision_traces_student_action CHECK (student_action IS NULL OR student_action IN ('ADD_CITATION', 'PARAPHRASE', 'QUALIFY', 'SYNTHESIZE', 'QUOTE', 'REMOVE', 'DISMISS_WITH_REASON')),
    CONSTRAINT chk_evidence_revision_traces_outcome CHECK (outcome IS NULL OR outcome IN ('RESOLVED', 'PARTIALLY_RESOLVED', 'UNRESOLVED', 'STALE')),
    CONSTRAINT chk_evidence_revision_traces_judgment CHECK (judgment IS NULL OR judgment IN ('EFFECTIVE', 'PARTIAL', 'INEFFECTIVE')),
    CONSTRAINT chk_evidence_revision_traces_linked_mode CHECK (linked_mode IS NULL OR linked_mode IN ('VERBATIM_CONTINUATION', 'REVISION_CHAIN')),
    CONSTRAINT chk_evidence_revision_traces_ai_recheck_judgment CHECK (ai_recheck_judgment IS NULL OR ai_recheck_judgment IN ('EFFECTIVE', 'PARTIAL', 'INEFFECTIVE')),
    FOREIGN KEY (round_id) REFERENCES citation_review_rounds(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES documents(id) ON DELETE SET NULL,
    FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE SET NULL,
    FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (linked_round_id) REFERENCES citation_review_rounds(id) ON DELETE SET NULL
);