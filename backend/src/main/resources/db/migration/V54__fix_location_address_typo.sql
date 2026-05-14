-- Adaugare coloana address daca lipseste, sau redenumire din adress/adresa daca exista
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='adress') THEN
ALTER TABLE locations RENAME COLUMN adress TO address;
ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='adresa') THEN
ALTER TABLE locations RENAME COLUMN adresa TO address;
ELSIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='address') THEN
ALTER TABLE locations ADD COLUMN address TEXT;
END IF;
END $$;

-- Asigurare PostGIS si coloana location (geography)
CREATE EXTENSION IF NOT EXISTS postgis;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='location') THEN
ALTER TABLE locations ADD COLUMN location geography(Point, 4326);
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_locations_location ON locations USING GIST (location);