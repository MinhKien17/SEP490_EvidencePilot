CREATE TABLE users (
    id BINARY(16) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMIN')),
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    email_verification_token_hash VARCHAR(255) UNIQUE,
    email_verification_token_expires_at DATETIME,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id BINARY(16) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'SUBMITTED_FOR_REVIEW', 'RETURNED', 'APPROVED')),
    target_standard VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE project_members (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER', 'INSTRUCTOR')),
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_project_members_unique (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE collections (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16),
    instructor_id BINARY(16) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE source_categories (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documents (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16),
    collection_id BINARY(16),
    source_category_id BINARY(16),
    uploaded_by BINARY(16) NOT NULL,
    doc_type VARCHAR(50) NOT NULL CHECK (doc_type IN ('PAPER', 'SOURCE')),
    file_url VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(255),
    file_size_bytes BIGINT,
    file_hash_sha256 VARCHAR(64),
    processing_status VARCHAR(50) NOT NULL CHECK (processing_status IN ('PENDING_UPLOAD', 'UPLOADED', 'METADATA_FETCHED', 'PDF_DOWNLOADED', 'QUEUED', 'PROCESSING', 'RAW_EXTRACTED', 'READY', 'COMPLETED', 'FAILED')),
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
    download_token VARCHAR(36),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_documents_project_id (project_id),
    INDEX idx_documents_collection_id (collection_id),
    INDEX idx_documents_source_category_id (source_category_id),
    INDEX idx_documents_file_hash_sha256 (file_hash_sha256),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE SET NULL,
    FOREIGN KEY (source_category_id) REFERENCES source_categories(id) ON DELETE SET NULL,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

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
    text TEXT NOT NULL,
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
    doi VARCHAR(255),
    edge_type VARCHAR(50),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE TABLE paper_sections (
    id BINARY(16) NOT NULL PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    assigned_user_id BINARY(16),
    section_order INT NOT NULL,
    section_title VARCHAR(255) NOT NULL,
    content_tex LONGTEXT NOT NULL,
    content_md_cache LONGTEXT,
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

CREATE TABLE claims (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    section_id BINARY(16),
    content TEXT NOT NULL,
    ai_confidence_score FLOAT,
    claim_version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE SET NULL
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_claim_evidence_mappings_unique (claim_id, document_chunk_id),
    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
    FOREIGN KEY (document_chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE,
    FOREIGN KEY (suggestion_id) REFERENCES ai_suggestions(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE evidence_edges (
    id BINARY(16) NOT NULL PRIMARY KEY,
    claim_id BINARY(16) NOT NULL,
    document_chunk_id BINARY(16) NOT NULL,
    verdict VARCHAR(255) NOT NULL,
    confidence_score FLOAT NOT NULL,
    explanation TEXT,
    missing_evidence TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
    FOREIGN KEY (document_chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE
);

CREATE TABLE section_feedback (
    id BINARY(16) NOT NULL PRIMARY KEY,
    section_id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    line_reference VARCHAR(100),
    content TEXT NOT NULL,
    resolved BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE feedback_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    project_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    instructor_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RETURNED', 'REVIEWED', 'REJECTED')),
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES feedback_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES paper_sections(id) ON DELETE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE CASCADE
);

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
