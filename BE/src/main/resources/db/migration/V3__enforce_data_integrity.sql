-- Business keys used as unique lookups or stable ordinals.
ALTER TABLE document_chunks
    ADD CONSTRAINT uq_document_chunks_document_index UNIQUE (document_id, chunk_index);

ALTER TABLE document_chunks
    DROP INDEX idx_document_chunks;

ALTER TABLE review_snapshots
    ADD CONSTRAINT uq_review_snapshots_lookup UNIQUE (project_id, style, input_fingerprint);

ALTER TABLE review_snapshots
    DROP INDEX idx_review_snapshot;

ALTER TABLE project_media
    ADD CONSTRAINT uq_project_media_storage UNIQUE (project_id, storage_key);

ALTER TABLE document_references
    ADD CONSTRAINT uq_document_references_order UNIQUE (document_id, edge_type, reference_index);

-- Required values already enforced by application write paths.
ALTER TABLE collections
    MODIFY title VARCHAR(255) NOT NULL,
    MODIFY active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE instructor_feedbacks
    MODIFY answered BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE system_notifications
    MODIFY is_read BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE documents
    MODIFY download_token VARCHAR(36) NOT NULL;

ALTER TABLE document_references
    MODIFY edge_type VARCHAR(50) NOT NULL,
    ADD CONSTRAINT chk_document_references_edge_type
        CHECK (edge_type IN ('REFERENCES', 'CITED_BY'));

-- Closed enum domains mirrored from the Java model.
ALTER TABLE projects
    ADD CONSTRAINT chk_projects_target_standard
        CHECK (target_standard IS NULL OR target_standard IN (
            'IEEE', 'ACM', 'SPRINGER_LNCS', 'APA', 'MLA', 'CUSTOM'
        ));

ALTER TABLE export_jobs
    ADD CONSTRAINT chk_export_jobs_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    ADD CONSTRAINT chk_export_jobs_format
        CHECK (format IN ('TEX', 'TRACEABILITY'));

ALTER TABLE ai_evaluation_jobs
    ADD CONSTRAINT chk_ai_evaluation_jobs_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    ADD CONSTRAINT chk_ai_evaluation_jobs_kind
        CHECK (kind IN ('SECTION_CITATION_REVIEW', 'SECTION_SUGGESTION'));
