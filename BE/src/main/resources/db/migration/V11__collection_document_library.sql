CREATE TABLE collection_documents (
    id BINARY(16) NOT NULL PRIMARY KEY,
    collection_id BINARY(16) NOT NULL,
    document_id BINARY(16) NOT NULL,
    added_by BINARY(16) NOT NULL,
    added_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_collection_documents_unique (collection_id, document_id),
    INDEX idx_collection_documents_document (document_id),
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (added_by) REFERENCES users(id) ON DELETE CASCADE
);
