CREATE TABLE audit_logs (
    id          BINARY(16) PRIMARY KEY,
    actor_id    BINARY(16) NOT NULL REFERENCES users(id),
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   BINARY(16) NOT NULL,
    old_value   JSON,
    new_value   JSON,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_actor (actor_id),
    INDEX idx_audit_occurred (occurred_at)
);
