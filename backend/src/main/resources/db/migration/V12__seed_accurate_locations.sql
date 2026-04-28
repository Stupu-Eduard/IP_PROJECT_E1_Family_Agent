-- Seed / update demo locations with more specific address text (better for geocoding).
-- Intentionally does NOT set PostGIS `location` (geography) column.
-- If you already ran a previous V12 on an existing DB, you must reset DB or run Flyway repair.

-- Update existing rows only if address is missing or too generic
UPDATE locations
SET
    adress = 'Soseaua Pantelimon 244, Sector 2',
    city = 'Bucuresti',
    country = 'Romania'
WHERE store = 'Kaufland';

UPDATE locations
SET
    adress = 'Soseaua Mihai Bravu 254, Sector 3',
    city = 'Bucuresti',
    country = 'Romania'
WHERE store = 'Benzinăria OMV';


UPDATE locations
SET
    adress = 'Bulevardul Decebal 6, Sector 3',
    city = 'Bucuresti',
    country = 'Romania'
WHERE store = 'Mega Image';

-- Insert rows if missing (fresh DB)
INSERT INTO locations (store, adress, city, country)
SELECT 'Kaufland', 'Soseaua Pantelimon 244, Sector 2', 'Bucuresti', 'Romania'
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE store = 'Kaufland');

INSERT INTO locations (store, adress, city, country)
SELECT 'Benzinăria OMV', 'Soseaua Mihai Bravu 254, Sector 3', 'Bucuresti', 'Romania'
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE store IN ('Benzinaria OMV', 'Benzinăria OMV'));

INSERT INTO locations (store, adress, city, country)
SELECT 'Mega Image', 'Bulevardul Decebal 6, Sector 3', 'Bucuresti', 'Romania'
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE store = 'Mega Image');
