-- Safe backfills required before stricter nullability and check constraints.
UPDATE collections
SET active = TRUE
WHERE active IS NULL;

UPDATE instructor_feedbacks
SET answered = FALSE
WHERE answered IS NULL;

UPDATE system_notifications
SET is_read = FALSE
WHERE is_read IS NULL;

UPDATE documents
SET download_token = UUID()
WHERE download_token IS NULL OR download_token = '';

-- Rows created before citation direction was introduced represent outgoing references.
UPDATE document_references
SET edge_type = 'REFERENCES'
WHERE edge_type IS NULL OR edge_type = '';

-- Review snapshots are a cache. Keep the newest row for each lookup key.
CREATE TEMPORARY TABLE duplicate_review_snapshots AS
SELECT id
FROM (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY project_id, style, input_fingerprint
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM review_snapshots
) ranked
WHERE rn > 1;

DELETE review_snapshots
FROM review_snapshots
JOIN duplicate_review_snapshots USING (id);

DROP TEMPORARY TABLE duplicate_review_snapshots;

-- A storage key identifies one object. project_media has no inbound foreign keys.
CREATE TEMPORARY TABLE duplicate_project_media AS
SELECT id
FROM (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY project_id, storage_key
               ORDER BY uploaded_at DESC, id DESC
           ) AS rn
    FROM project_media
) ranked
WHERE rn > 1;

DELETE project_media
FROM project_media
JOIN duplicate_project_media USING (id);

DROP TEMPORARY TABLE duplicate_project_media;

-- reference_index is an ordinal only. Normalize it deterministically per edge type.
CREATE TEMPORARY TABLE normalized_reference_indexes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    reference_index INT NOT NULL
);

INSERT INTO normalized_reference_indexes (id, reference_index)
SELECT id,
       ROW_NUMBER() OVER (
           PARTITION BY document_id, edge_type
           ORDER BY reference_index, id
       ) - 1
FROM document_references;

UPDATE document_references reference_row
JOIN normalized_reference_indexes normalized USING (id)
SET reference_row.reference_index = normalized.reference_index;

DROP TEMPORARY TABLE normalized_reference_indexes;
