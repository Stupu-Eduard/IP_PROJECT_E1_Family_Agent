CREATE TABLE family_invitations (
                                    id          BIGSERIAL PRIMARY KEY,
                                    family_id   BIGINT       NOT NULL REFERENCES families(id) ON DELETE CASCADE,
                                    invitee_email VARCHAR(255) NOT NULL,
                                    role        VARCHAR(50)  NOT NULL DEFAULT 'Child',
                                    invited_by  BIGINT       NOT NULL REFERENCES users(id),
                                    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                                    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    UNIQUE (family_id, invitee_email)
);