CREATE TABLE IF NOT EXISTS answers (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id BIGINT REFERENCES users(id),
    animal VARCHAR(255),
    color VARCHAR(255),
    street VARCHAR(255)
    );