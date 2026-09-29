-- 47. Per-user recent destinations (private navigation shortcuts).
-- Browser-local history would persist on shared machines and never sync
-- across devices, so destinations live server-side, scoped to the owning user.
-- A nil ref_id marks destination kinds without a referenced row.
CREATE TABLE recent_destinations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    kind VARCHAR(30) NOT NULL,
    ref_id BINARY(16) NOT NULL,
    tab VARCHAR(50) NULL,
    label VARCHAR(255) NULL,
    context VARCHAR(255) NULL,
    link VARCHAR(500) NOT NULL,
    last_opened_at DATETIME NOT NULL,
    CONSTRAINT chk_recent_kind CHECK (kind IN ('PROJECT', 'COLLECTION', 'SOURCE_LIBRARY')),
    CONSTRAINT fk_recent_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_recent_destination UNIQUE (user_id, kind, ref_id, tab)
);
CREATE INDEX idx_recent_user_opened ON recent_destinations (user_id, last_opened_at);
