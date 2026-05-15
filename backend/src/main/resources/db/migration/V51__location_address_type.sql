

-- Adaugare coloana address daca lipseste, sau redenumire din adress/adresa daca exista
DO $$
BEGIN
    -- Daca exista adress si NU exista address, redenumeste
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='adress')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='address') THEN
        ALTER TABLE locations RENAME COLUMN adress TO address;
    -- Daca exista adresa si NU exista address, redenumeste
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='adresa')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='address') THEN
        ALTER TABLE locations RENAME COLUMN adresa TO address;
    -- Daca exista ADRESS si exista si ADDRESS, copiaza datele lipsa si sterge adress
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='adress')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='address') THEN
        UPDATE locations SET address = adress WHERE address IS NULL OR address = '';
        ALTER TABLE locations DROP COLUMN adress;
    -- Daca nu exista deloc, adauga
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
