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
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
    title VARCHAR(255),
    description TEXT,
    category_id BINARY(16),
    active BOOLEAN DEFAULT TRUE,
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
    download_token VARCHAR(36),
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
    INDEX idx_document_chunks (document_id, chunk_index),
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
    edge_type VARCHAR(50),
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
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- 6. CLAIMS & AI TRACEABILITY
-- ==========================================
CREATE TABLE claims (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    section_id BINARY(16),
    created_by BINARY(16),
    content TEXT NOT NULL,
    ai_confidence_score FLOAT,
    claim_quality_score FLOAT,
    functional_type VARCHAR(50) CHECK (functional_type IN ('EMPIRICAL','THEORETICAL','METHODOLOGICAL','ANALYTICAL','APPLIED')),
    claim_version INT NOT NULL DEFAULT 1,
    opt_version BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_claims_project_id (project_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE ai_suggestions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    claim_id BINARY(16) NOT NULL,
    document_chunk_id BINARY(16) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'INVALIDATED')),
    instructor_rejected BOOLEAN NOT NULL DEFAULT FALSE,
    score FLOAT,
    explanation TEXT,
    claim_version INT NOT NULL,
    model_name VARCHAR(255),
    model_version VARCHAR(255),
    prompt_version VARCHAR(255),
    rubric_version VARCHAR(255),
    evaluated_at DATETIME,
    score_breakdown JSON,
    relation VARCHAR(50) CHECK (relation IN ('SUPPORTS', 'CONTRADICTS', 'NEUTRAL', 'EXTENDS', 'DETAILS', 'GENERALIZES')),
    strength_score INT,
    strength_band VARCHAR(20) CHECK (strength_band IN ('HIGH', 'MEDIUM', 'LOW')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
    FOREIGN KEY (document_chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE
);

CREATE TABLE claim_evidence_mappings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    claim_id BINARY(16) NOT NULL,
    document_chunk_id BINARY(16) NOT NULL,
    suggestion_id BINARY(16),
    created_by BINARY(16) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    instructor_rejected BOOLEAN NOT NULL DEFAULT FALSE,
    relation VARCHAR(50) CHECK (relation IN ('SUPPORTS', 'CONTRADICTS', 'NEUTRAL', 'EXTENDS', 'DETAILS', 'GENERALIZES')),
    strength_score INT,
    strength_band VARCHAR(20) CHECK (strength_band IN ('HIGH', 'MEDIUM', 'LOW')),
    review_status VARCHAR(50) CHECK (review_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    reviewed_by BINARY(16),
    reviewed_at DATETIME,
    review_note TEXT,
    relation_override VARCHAR(50) CHECK (relation_override IN ('SUPPORTS', 'CONTRADICTS', 'NEUTRAL', 'EXTENDS', 'DETAILS', 'GENERALIZES')),
    score_breakdown JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_claim_evidence_mappings_unique (claim_id, document_chunk_id),
    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
    FOREIGN KEY (document_chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE,
    FOREIGN KEY (suggestion_id) REFERENCES ai_suggestions(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
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
    answered BOOLEAN DEFAULT FALSE,
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
    is_read BOOLEAN DEFAULT FALSE,
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
    INDEX idx_review_snapshot (project_id, style, input_fingerprint),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- ==========================================
-- 14. AI EVALUATION JOBS (async claim-quality / match evaluation)
-- ==========================================
CREATE TABLE ai_evaluation_jobs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    kind VARCHAR(50) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    result_json LONGTEXT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    INDEX idx_ai_eval_project (project_id),
    INDEX idx_ai_eval_status (status),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
