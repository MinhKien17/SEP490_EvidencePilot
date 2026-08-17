ALTER TABLE users DROP INDEX student_code;

-- allow re-creating a student code once its previous user is soft-deleted
-- (DELETED rows evaluate to NULL so the unique index lets them repeat)
CREATE UNIQUE INDEX idx_users_student_code_active
    ON users ((CASE WHEN account_status = 'DELETED' THEN NULL ELSE student_code END));
