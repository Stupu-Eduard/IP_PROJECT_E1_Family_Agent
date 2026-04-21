INSERT INTO categories (name) VALUES ('Mâncare'), ('Transport'), ('Divertisment'), ('Sănătate');

INSERT INTO expenses (amount, currency, description, expense_date, source_type, user_id, family_id)
VALUES (150.50, 'RON', 'Cumpărături săptămânale', CURRENT_TIMESTAMP, 'MANUAL', 1, 1);

INSERT INTO expenses (amount, currency, description, expense_date, source_type, user_id, family_id)
VALUES (45.00, 'RON', 'Benzină', CURRENT_TIMESTAMP, 'MANUAL', 1, 1);

INSERT INTO expense_items (amount, description, item_name, quantity, expense_id)
VALUES (10.50, 'Pâine proaspătă', 'Pâine', 2, (SELECT id FROM expenses WHERE description = 'Cumpărături săptămânale' LIMIT 1));

INSERT INTO expense_items (amount, description, item_name, quantity, expense_id)
VALUES (140.00, 'Diverse alimente', 'Alimente', 1, (SELECT id FROM expenses WHERE description = 'Cumpărături săptămânale' LIMIT 1));