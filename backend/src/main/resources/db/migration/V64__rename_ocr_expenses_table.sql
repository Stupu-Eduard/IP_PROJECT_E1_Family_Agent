-- Rename expenses table used by ExpenseOCREntity to ocr_expenses to avoid collision with ExpenseEntity
-- This separates OCR staging data from main expense records

-- Only rename if the old table exists and new one doesn't
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_schema = 'public' AND table_name = 'expenses') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables 
                       WHERE table_schema = 'public' AND table_name = 'ocr_expenses')
    THEN
        -- Check if expenses table has OCR-specific columns (ocr_text, raw_input) 
        -- to confirm it's the OCR table and not the main expenses table
        IF EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' AND table_name = 'expenses' 
                   AND column_name = 'ocr_text')
        THEN
            ALTER TABLE expenses RENAME TO ocr_expenses;
        END IF;
    END IF;
END $$;
