CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS location geography(Point, 4326);

CREATE INDEX IF NOT EXISTS idx_locations_location
    ON locations
    USING GIST (location);