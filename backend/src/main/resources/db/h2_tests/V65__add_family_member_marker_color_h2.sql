-- H2 test DB migration to add marker_color to family_members
ALTER TABLE family_members ADD COLUMN IF NOT EXISTS marker_color VARCHAR(64);
