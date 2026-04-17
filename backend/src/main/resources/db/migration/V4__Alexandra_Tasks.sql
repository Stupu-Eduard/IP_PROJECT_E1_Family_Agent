--Redenumire col sum -> amount
ALTER TABLE expenses RENAME COLUMN sum TO amount;

-- Implementare Locations (Update conform UML)
ALTER TABLE locations ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE locations ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE locations RENAME COLUMN name TO store;

-- Update Expenses (Relațiile cerute: Family, User, Location)
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS family_id BIGINT REFERENCES families(id);
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id);
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS location_id BIGINT REFERENCES locations(id);

-- 4. Design Expense Items (Linia de bon + Relatia cu Category)
CREATE TABLE IF NOT EXISTS expense_items (
    id BIGSERIAL PRIMARY KEY,
    amount DECIMAL(19, 2) NOT NULL,
    description TEXT,
    expense_id BIGINT NOT NULL REFERENCES expenses(id)
);