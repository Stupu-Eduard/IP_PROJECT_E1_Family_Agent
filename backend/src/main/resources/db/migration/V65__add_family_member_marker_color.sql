-- Adds marker_color to family_members for child marker persistence
ALTER TABLE family_members ADD COLUMN IF NOT EXISTS marker_color VARCHAR(64);

-- No backfill performed here; application will assign colors when needed.
