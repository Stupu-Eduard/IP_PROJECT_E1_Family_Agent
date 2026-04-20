
CREATE TABLE alerts (
                        id                  BIGSERIAL PRIMARY KEY,
                        child_id            BIGINT NOT NULL,
                        parent_id           BIGINT NOT NULL,
                        message             TEXT NOT NULL,
                        restricted_category VARCHAR(100) NOT NULL,
                        timestamp           TIMESTAMP NOT NULL,
                        read                BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_alerts_parent_id ON alerts(parent_id);