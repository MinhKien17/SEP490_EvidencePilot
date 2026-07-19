CREATE TABLE project_documents (
    id          BINARY(16) PRIMARY KEY,
    project_id  BINARY(16) NOT NULL,
    document_id BINARY(16) NOT NULL,
    shared_by   BINARY(16) NOT NULL,
    shared_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_document (project_id, document_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_by) REFERENCES users(id) ON DELETE CASCADE
);
