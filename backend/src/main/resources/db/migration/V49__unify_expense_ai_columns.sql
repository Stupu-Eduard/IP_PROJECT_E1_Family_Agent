-- Add official AI pipeline columns to the expenses table
-- These columns were previously auto-created by Hibernate ddl-auto: update
-- Now they are formally managed by Flyway

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS ai_category VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_location VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_person VARCHAR(255),
    ADD COLUMN IF NOT EXISTS raw_input VARCHAR(1000);

-- Migrate data from old phantom columns (created by Hibernate) to new official columns
-- This preserves existing AI-extracted data after the entity migration
UPDATE expenses
    SET ai_category = category
    WHERE category IS NOT NULL
      AND ai_category IS NULL;

UPDATE expenses
    SET ai_location = location
    WHERE location IS NOT NULL
      AND ai_location IS NULL;

UPDATE expenses
    SET ai_person = person
    WHERE person IS NOT NULL
      AND ai_person IS NULL;
