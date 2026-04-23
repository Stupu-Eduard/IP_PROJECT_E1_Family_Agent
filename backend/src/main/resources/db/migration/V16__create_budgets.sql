CREATE TABLE IF NOT EXISTS budgets (
                                       id BIGSERIAL PRIMARY KEY,
                                       amount DECIMAL(19, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    category_id BIGINT REFERENCES categories(id),
    family_id BIGINT REFERENCES families(id)
    );