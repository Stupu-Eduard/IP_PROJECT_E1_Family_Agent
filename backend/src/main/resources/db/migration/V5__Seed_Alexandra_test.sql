
DELETE FROM expense_items;

INSERT INTO locations (store, city, country)
VALUES ('Mega Image', 'Bucuresti', 'Romania');

INSERT INTO expenses (amount, description, date, user_id, family_id, location_id, category_id)
VALUES (
           85.50,
           'Cumparaturi mic dejun',
           '2026-03-27 10:00:00',
       (SELECT id FROM users LIMIT 1),
       (SELECT id FROM families LIMIT 1),
       (SELECT id FROM locations WHERE store = 'Mega Image' LIMIT 1),
       (SELECT id FROM categories LIMIT 1)
    );

INSERT INTO expense_items (amount, description, expense_id)
VALUES (
           12.30,
           'Iaurt Grecesc',
           (SELECT id FROM expenses WHERE description = 'Cumparaturi mic dejun' LIMIT 1)
    );

INSERT INTO expense_items (amount, description, expense_id)
VALUES (
           45.20,
           'Cafea Boabe 500g',
           (SELECT id FROM expenses WHERE description = 'Cumparaturi mic dejun' LIMIT 1)
    );

INSERT INTO expense_items (amount, description, expense_id)
VALUES (
           28.00,
           'Cereale Integrale',
           (SELECT id FROM expenses WHERE description = 'Cumparaturi mic dejun' LIMIT 1)
    );