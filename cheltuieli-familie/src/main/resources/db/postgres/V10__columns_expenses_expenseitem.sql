--tabelul nou de cheltuieli (Expenses)
ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS expense_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'RON',
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'manual',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

--schimbare coloana 'date' in 'expense_date' si sters pe cea veche
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='expenses' AND column_name='date') THEN
UPDATE expenses SET expense_date = date WHERE expense_date IS NULL;
END IF;
END $$;

--tabelul actualizat (Expense Items)
ALTER TABLE expense_items
    ADD COLUMN IF NOT EXISTS item_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS quantity DECIMAL(19, 2) DEFAULT 1,
    ADD COLUMN IF NOT EXISTS raw_text TEXT,
    ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES categories(id);