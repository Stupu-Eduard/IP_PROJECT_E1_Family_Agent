CREATE TABLE families (
    id BIGSERIAL PRIMARY KEY,
    nume VARCHAR(100) NOT NULL
);

CREATE TABLE categorii (
    id BIGSERIAL PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    parent_id BIGINT REFERENCES categorii(id)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    parola VARCHAR(255) NOT NULL,
    family_id BIGINT REFERENCES families(id)
);

CREATE TABLE family_members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    family_id BIGINT REFERENCES families(id),
    rol VARCHAR(50) NOT NULL
);

CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    adresa TEXT
);

CREATE TABLE cheltuieli (
    id BIGSERIAL PRIMARY KEY,
    suma DECIMAL(19, 2) NOT NULL CHECK (suma > 0),
    descriere TEXT,
    data_cheltuiala TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    categorie_id BIGINT REFERENCES categorii(id),
    location_id BIGINT REFERENCES locations(id),
    user_id BIGINT REFERENCES users(id)
);