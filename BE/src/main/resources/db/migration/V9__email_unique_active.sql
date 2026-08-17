ALTER TABLE users DROP INDEX email;

-- allow re-creating an email once its previous row is soft-deleted
-- (DELETED rows evaluate to NULL so the unique index lets them repeat)
CREATE UNIQUE INDEX idx_users_email_active
    ON users ((CASE WHEN account_status = 'DELETED' THEN NULL ELSE email END));