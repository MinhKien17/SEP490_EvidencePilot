CREATE TABLE user_sessions (
    jti VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NULL,
    issued_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL
);