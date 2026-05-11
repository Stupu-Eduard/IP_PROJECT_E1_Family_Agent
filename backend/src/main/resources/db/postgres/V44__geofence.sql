CREATE TABLE IF NOT EXISTS geofence_zones (
                                              id          BIGSERIAL PRIMARY KEY,
                                              name        VARCHAR(255) NOT NULL,
    description TEXT,
    area        GEOMETRY(Polygon, 4326),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE INDEX IF NOT EXISTS idx_geofence_zones_area ON geofence_zones USING GIST (area);