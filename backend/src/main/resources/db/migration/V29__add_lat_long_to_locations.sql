-- Adaugare coloane pentru coordonate geografice brute inainte de popularea din V32
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
