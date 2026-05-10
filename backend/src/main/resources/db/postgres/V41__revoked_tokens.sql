CREATE TABLE revoked_tokens (
    jti VARCHAR(255) PRIMARY KEY,
    revoked_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
