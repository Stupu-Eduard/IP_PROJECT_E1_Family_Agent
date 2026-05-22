-- Add idempotency key to expenses table to prevent duplicate entries from OCR retry logic
ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) UNIQUE;

-- Create index for fast lookup during duplicate checks
CREATE INDEX IF NOT EXISTS idx_expenses_idempotency_key ON expenses(idempotency_key);
