BEGIN;

TRUNCATE TABLE
    budgets,
    expense_items,
    expenses,
    family_members,
    locations,
    categories,
    users,
    families
RESTART IDENTITY CASCADE;

INSERT INTO categories (name, description, is_active)
VALUES
    ('Mâncare', NULL, false),
    ('Chirie', NULL, false),
    ('Transport', NULL, false),
    ('Divertisment', NULL, false),
    ('Sănătate', NULL, false);

INSERT INTO families (name, created_at)
VALUES ('Familia Ionescu', NULL);

INSERT INTO users (name, email, password_h, created_at)
VALUES ('Ion Ionescu', 'ion@email.com', 'parola_securizata_123', NULL);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'ADMIN'
FROM users u
         JOIN families f ON f.name = 'Familia Ionescu'
WHERE u.email = 'ion@email.com';

INSERT INTO locations (store, address, city, country)
VALUES
    ('Kaufland', 'Soseaua Pantelimon 244, Sector 2', 'Bucuresti', 'Romania'),
    ('Benzinăria OMV', 'Soseaua Mihai Bravu 254, Sector 3', 'Bucuresti', 'Romania'),
    ('Mega Image', 'Bulevardul Decebal 6, Sector 3', 'Bucuresti', 'Romania');

INSERT INTO expenses (amount, description, expense_date, category_id, location_id, user_id)
VALUES (
           150.00,
           'Cumpărături supermarket',
           CURRENT_TIMESTAMP,
           (SELECT id FROM categories WHERE name = 'Mâncare'),
           (SELECT id FROM locations WHERE store = 'Kaufland' LIMIT 1),
       (SELECT id FROM users WHERE email = 'ion@email.com')
    );

INSERT INTO expenses (amount, description, expense_date, user_id, family_id, location_id, category_id)
VALUES (
           85.50,
           'Cumparaturi mic dejun',
           '2026-03-27 10:00:00',
           (SELECT id FROM users WHERE email = 'ion@email.com'),
           (SELECT id FROM families WHERE name = 'Familia Ionescu'),
           (SELECT id FROM locations WHERE store = 'Mega Image' LIMIT 1),
       (SELECT id FROM categories WHERE name = 'Mâncare')
    );

INSERT INTO expense_items (amount, description, expense_id)
VALUES
    (12.30, 'Iaurt Grecesc', (SELECT id FROM expenses WHERE description = 'Cumparaturi mic dejun' LIMIT 1)),
    (45.20, 'Cafea Boabe 500g', (SELECT id FROM expenses WHERE description = 'Cumparaturi mic dejun' LIMIT 1)),
    (28.00, 'Cereale Integrale', (SELECT id FROM expenses WHERE description = 'Cumparaturi mic dejun' LIMIT 1));

INSERT INTO expenses (amount, currency, description, expense_date, source_type, user_id, family_id)
VALUES
    (150.50, 'RON', 'Cumpărături săptămânale', CURRENT_TIMESTAMP, 'MANUAL', (SELECT id FROM users WHERE email = 'ion@email.com'), (SELECT id FROM families WHERE name = 'Familia Ionescu')),
    (45.00, 'RON', 'Benzină', CURRENT_TIMESTAMP, 'MANUAL', (SELECT id FROM users WHERE email = 'ion@email.com'), (SELECT id FROM families WHERE name = 'Familia Ionescu'));

INSERT INTO expense_items (amount, description, item_name, quantity, expense_id)
VALUES
    (10.50, 'Pâine proaspătă', 'Pâine', 2, (SELECT id FROM expenses WHERE description = 'Cumpărături săptămânale' LIMIT 1)),
    (140.00, 'Diverse alimente', 'Alimente', 1, (SELECT id FROM expenses WHERE description = 'Cumpărături săptămânale' LIMIT 1));



-- SOURCE: V31__seed_user_family_fmembers_cat.sql
INSERT INTO users (name, email, password_h, created_at)
SELECT 'Ana Popescu',   'ana.popescu@email.com',   '$2a$10$dummyHashAnaPopescu000000000000000000000000000000000', '2024-01-10'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'ana.popescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Mihai Popescu', 'mihai.popescu@email.com', '$2a$10$dummyHashMihaiPopescu00000000000000000000000000000000', '2024-01-10'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'mihai.popescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Sofia Popescu', 'sofia.popescu@email.com', '$2a$10$dummyHashSofiaPopescu00000000000000000000000000000000', '2024-01-10'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'sofia.popescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Radu Ionescu',  'radu.ionescu@email.com',  '$2a$10$dummyHashRaduIonescu000000000000000000000000000000000', '2024-02-05'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'radu.ionescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Elena Ionescu', 'elena.ionescu@email.com', '$2a$10$dummyHashElenaIonescu00000000000000000000000000000000', '2024-02-05'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'elena.ionescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Andrei Ionescu','andrei.ionescu@email.com','$2a$10$dummyHashAndreiIonescu0000000000000000000000000000000', '2024-02-05'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'andrei.ionescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Maria Popa',    'maria.popa@email.com',    '$2a$10$dummyHashMariaPopa000000000000000000000000000000000000', '2024-03-15'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'maria.popa@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Cristian Popa', 'cristian.popa@email.com', '$2a$10$dummyHashCristianPopa00000000000000000000000000000000', '2024-03-15'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'cristian.popa@email.com');


INSERT INTO families (name, created_at)
SELECT 'Familia Popescu', '2024-01-10'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Popescu');

INSERT INTO families (name, created_at)
SELECT 'Familia Ionescu', '2024-02-05'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Ionescu');

INSERT INTO families (name, created_at)
SELECT 'Familia Popa', '2024-03-15'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Popa');



INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'ana.popescu@email.com' AND f.name = 'Familia Popescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'mihai.popescu@email.com' AND f.name = 'Familia Popescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'sofia.popescu@email.com' AND f.name = 'Familia Popescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);


INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'ion@email.com' AND f.name = 'Familia Ionescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'radu.ionescu@email.com' AND f.name = 'Familia Ionescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'elena.ionescu@email.com' AND f.name = 'Familia Ionescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'andrei.ionescu@email.com' AND f.name = 'Familia Ionescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);


INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'maria.popa@email.com' AND f.name = 'Familia Popa'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'cristian.popa@email.com' AND f.name = 'Familia Popa'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);


INSERT INTO categories (name, description, is_active)
SELECT 'Mâncare', 'Cheltuieli legate de alimente si masa', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mâncare');

INSERT INTO categories (name, description, is_active)
SELECT 'Transport', 'Cheltuieli legate de deplasare', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Transport');

INSERT INTO categories (name, description, is_active)
SELECT 'Sănătate', 'Cheltuieli medicale si de sanatate', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Sănătate');

INSERT INTO categories (name, description, is_active)
SELECT 'Divertisment', 'Cheltuieli de timp liber si activitati', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Divertisment');

INSERT INTO categories (name, description, is_active)
SELECT 'Educatie', 'Cheltuieli scolare si de formare', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Educatie');

INSERT INTO categories (name, description, is_active)
SELECT 'Shopping', 'Cumparaturi diverse', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Shopping');

INSERT INTO categories (name, description, is_active)
SELECT 'Numerar', 'Retrageri si plati in numerar', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Numerar');

INSERT INTO categories (name, description, is_active)
SELECT 'Servicii', 'Abonamente si servicii platite', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Servicii');

INSERT INTO categories (name, description, is_active)
SELECT 'Pentru casa', 'Cheltuieli pentru locuinta', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Pentru casa');

INSERT INTO categories (name, description, is_active)
SELECT 'Altele', 'Cheltuieli diverse neclasificate', true
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Altele');

-- SUBCATEGORII: Mâncare
INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Supermarket', 'Cumparaturi din supermarket', true,
       (SELECT id FROM categories WHERE name = 'Mâncare')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Supermarket');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Restaurant', 'Masa la restaurant sau fast-food', true,
       (SELECT id FROM categories WHERE name = 'Mâncare')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Restaurant');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Cafenea', 'Cafea si bauturi', true,
       (SELECT id FROM categories WHERE name = 'Mâncare')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Cafenea');

-- SUBCATEGORII: Transport
INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Carburant', 'Benzina, motorina, GPL', true,
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Carburant');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Taxi', 'Taxi si ride-sharing (Uber, Bolt)', true,
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Taxi');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Transport public', 'Bilete si abonamente STB, CFR', true,
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Transport public');

-- SUBCATEGORII: Sănătate
INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Medicamente', 'Medicamente si suplimente', true,
       (SELECT id FROM categories WHERE name = 'Sănătate')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Medicamente');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Consultatii', 'Consultatii si analize medicale', true,
       (SELECT id FROM categories WHERE name = 'Sănătate')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Consultatii');

-- SUBCATEGORII: Educatie
INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Rechizite', 'Rechizite si materiale scolare', true,
       (SELECT id FROM categories WHERE name = 'Educatie')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Rechizite');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Cursuri', 'Cursuri si training-uri', true,
       (SELECT id FROM categories WHERE name = 'Educatie')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Cursuri');

-- SUBCATEGORII: Divertisment
INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Streaming', 'Netflix, Spotify, abonamente online', true,
       (SELECT id FROM categories WHERE name = 'Divertisment')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Streaming');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Cinema', 'Bilete cinema si teatru', true,
       (SELECT id FROM categories WHERE name = 'Divertisment')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Cinema');

-- SUBCATEGORII: Servicii
INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Utilitati', 'Curent, apa, gaz, internet', true,
       (SELECT id FROM categories WHERE name = 'Servicii')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Utilitati');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Telefonie', 'Abonament telefon mobil', true,
       (SELECT id FROM categories WHERE name = 'Servicii')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Telefonie');

-- SUBCATEGORII: Pentru casa
INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Chirie', 'Chirie si rate', true,
       (SELECT id FROM categories WHERE name = 'Pentru casa')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Chirie');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Curatenie', 'Produse de curatenie si uz casnic', true,
       (SELECT id FROM categories WHERE name = 'Pentru casa')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Curatenie');

-- descriere pentru categorii
UPDATE categories SET description = 'Cheltuieli legate de alimente si masa', is_active = true
WHERE name = 'Mâncare' AND (description IS NULL OR description = '');

UPDATE categories SET description = 'Cheltuieli legate de deplasare', is_active = true
WHERE name = 'Transport' AND (description IS NULL OR description = '');

UPDATE categories SET description = 'Cheltuieli medicale si de sanatate', is_active = true
WHERE name = 'Sănătate' AND (description IS NULL OR description = '');

UPDATE categories SET description = 'Cheltuieli de timp liber si activitati', is_active = true
WHERE name = 'Divertisment' AND (description IS NULL OR description = '');

-- SOURCE: V32__populate_expenses.sql
-- 1. POPULARE LOCATIONS
INSERT INTO locations (store, city, country, latitude, longitude) VALUES
                                                                      ('Lidl Iasi', 'Iasi', 'Romania', 47.1585, 27.5852),
                                                                      ('Kaufland Pacurari', 'Iasi', 'Romania', 47.1720, 27.5500),
                                                                      ('Uber / Bolt', 'Iasi', 'Romania', 47.1610, 27.5920),
                                                                      ('Farmacie Catena', 'Iasi', 'Romania', 47.1650, 27.5800),
                                                                      ('Restaurant Vivo', 'Iasi', 'Romania', 47.1550, 27.6000),
                                                                      ('Starbucks Palas', 'Iasi', 'Romania', 47.1565, 27.5875),
                                                                      ('Cinema City', 'Iasi', 'Romania', 47.1540, 27.5890);

-- 2 & 3. POPULARE EXPENSES SI EXPENSE_ITEMS

-- SEPTEMBRIE 2025
INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1001, 185.00, '2025-09-15 10:30:00', (SELECT id FROM users WHERE email = 'ana.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Lidl Iasi' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Paine Feliata', 10.00, 1001, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Lapte 3.5%', 15.00, 1001, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Oua Caserola', 10.00, 1001, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Detergent Vase', 25.00, 1001, (SELECT id FROM categories WHERE name = 'Curatenie')),
                                                                           ('Sirop Tuse', 125.00, 1001, (SELECT id FROM categories WHERE name = 'Medicamente'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1002, 25.00, '2025-09-20 18:00:00', (SELECT id FROM users WHERE email = 'sofia.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Uber / Bolt' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Cursa scoala', 18.00, 1002, (SELECT id FROM categories WHERE name = 'Taxi')),
                                                                           ('Bacsis sofer', 7.00, 1002, (SELECT id FROM categories WHERE name = 'Taxi'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1003, 250.00, '2025-09-22 14:00:00', (SELECT id FROM users WHERE email = 'maria.popa@email.com'), (SELECT id FROM locations WHERE store = 'Lidl Iasi' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Faina', 30.00, 1003, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Carne de pui', 120.00, 1003, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Saci menajeri', 40.00, 1003, (SELECT id FROM categories WHERE name = 'Curatenie')),
                                                                           ('Bureti de vase', 60.00, 1003, (SELECT id FROM categories WHERE name = 'Curatenie'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1004, 120.00, '2025-09-28 19:30:00', (SELECT id FROM users WHERE email = 'radu.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Cinema City' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Bilet Adult', 40.00, 1004, (SELECT id FROM categories WHERE name = 'Cinema')),
                                                                           ('Bilet Copil', 30.00, 1004, (SELECT id FROM categories WHERE name = 'Cinema')),
                                                                           ('Popcorn Mare', 30.00, 1004, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Suc Cola', 20.00, 1004, (SELECT id FROM categories WHERE name = 'Restaurant'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1005, 150.00, '2025-09-30 20:00:00', (SELECT id FROM users WHERE email = 'mihai.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Restaurant Vivo' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Coaste de porc', 80.00, 1005, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Cartofi prajiti', 20.00, 1005, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Salata Coleslaw', 20.00, 1005, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Bere Neagra', 30.00, 1005, (SELECT id FROM categories WHERE name = 'Restaurant'));


-- OCTOMBRIE 2025
INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1006, 320.00, '2025-10-05 17:45:00', (SELECT id FROM users WHERE email = 'radu.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Kaufland Pacurari' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Carne Vita', 120.00, 1006, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Rosii', 30.00, 1006, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Castraveti', 20.00, 1006, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Cartofi', 30.00, 1006, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Scutece', 120.00, 1006, (SELECT id FROM categories WHERE name = 'Curatenie'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1007, 45.00, '2025-10-14 15:20:00', (SELECT id FROM users WHERE email = 'elena.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Starbucks Palas' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Frappuccino', 25.00, 1007, (SELECT id FROM categories WHERE name = 'Cafenea')),
                                                                           ('Briosa Ciocolata', 20.00, 1007, (SELECT id FROM categories WHERE name = 'Cafenea'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1008, 180.00, '2025-10-18 10:00:00', (SELECT id FROM users WHERE email = 'cristian.popa@email.com'), (SELECT id FROM locations WHERE store = 'Kaufland Pacurari' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Peste proaspat', 80.00, 1008, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Orez', 20.00, 1008, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Legume congelate', 40.00, 1008, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Inghetata', 40.00, 1008, (SELECT id FROM categories WHERE name = 'Supermarket'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1009, 22.00, '2025-10-22 14:00:00', (SELECT id FROM users WHERE email = 'elena.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Uber / Bolt' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Cursa intoarcere', 18.00, 1009, (SELECT id FROM categories WHERE name = 'Taxi')),
                                                                           ('Bacsis sofer', 4.00, 1009, (SELECT id FROM categories WHERE name = 'Taxi'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1010, 85.00, '2025-10-28 09:30:00', (SELECT id FROM users WHERE email = 'ana.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Farmacie Catena' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Paracetamol', 35.00, 1010, (SELECT id FROM categories WHERE name = 'Medicamente')),
                                                                           ('Vitamina C', 30.00, 1010, (SELECT id FROM categories WHERE name = 'Medicamente')),
                                                                           ('Plasturi', 20.00, 1010, (SELECT id FROM categories WHERE name = 'Medicamente'));


-- NOIEMBRIE 2025
INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1011, 210.00, '2025-11-10 11:00:00', (SELECT id FROM users WHERE email = 'maria.popa@email.com'), (SELECT id FROM locations WHERE store = 'Farmacie Catena' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Analize de sange', 150.00, 1011, (SELECT id FROM categories WHERE name = 'Consultatii')),
                                                                           ('Magneziu', 35.00, 1011, (SELECT id FROM categories WHERE name = 'Medicamente')),
                                                                           ('Calciu', 25.00, 1011, (SELECT id FROM categories WHERE name = 'Medicamente'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1012, 135.00, '2025-11-15 19:30:00', (SELECT id FROM users WHERE email = 'cristian.popa@email.com'), (SELECT id FROM locations WHERE store = 'Restaurant Vivo' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Burger Special', 45.00, 1012, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Cartofi Wedges', 20.00, 1012, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Bere IPA', 35.00, 1012, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Bere Blonda', 35.00, 1012, (SELECT id FROM categories WHERE name = 'Restaurant'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1013, 200.00, '2025-11-18 20:00:00', (SELECT id FROM users WHERE email = 'ana.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Restaurant Vivo' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Paste Carbonara', 60.00, 1013, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Pizza Diavola', 50.00, 1013, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Tiramisu', 30.00, 1013, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Limonada', 30.00, 1013, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Apa Plata', 30.00, 1013, (SELECT id FROM categories WHERE name = 'Restaurant'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1014, 45.00, '2025-11-22 17:00:00', (SELECT id FROM users WHERE email = 'andrei.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Cinema City' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Bilet Film', 25.00, 1014, (SELECT id FROM categories WHERE name = 'Cinema')),
                                                                           ('Nachos', 20.00, 1014, (SELECT id FROM categories WHERE name = 'Restaurant'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1015, 60.00, '2025-11-28 10:00:00', (SELECT id FROM users WHERE email = 'maria.popa@email.com'), (SELECT id FROM locations WHERE store = 'Starbucks Palas' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Caffe Latte', 20.00, 1015, (SELECT id FROM categories WHERE name = 'Cafenea')),
                                                                           ('Cappuccino', 20.00, 1015, (SELECT id FROM categories WHERE name = 'Cafenea')),
                                                                           ('Croissant cu unt', 10.00, 1015, (SELECT id FROM categories WHERE name = 'Cafenea')),
                                                                           ('Croissant ciocolata', 10.00, 1015, (SELECT id FROM categories WHERE name = 'Cafenea'));

-- DECEMBRIE 2025
INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1016, 30.00, '2025-12-02 08:30:00', (SELECT id FROM users WHERE email = 'mihai.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Uber / Bolt' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Cursa serviciu', 25.00, 1016, (SELECT id FROM categories WHERE name = 'Taxi')),
                                                                           ('Taxa vreme rea', 5.00, 1016, (SELECT id FROM categories WHERE name = 'Taxi'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1017, 70.00, '2025-12-10 20:00:00', (SELECT id FROM users WHERE email = 'sofia.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Cinema City' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Bilet Film 3D', 40.00, 1017, (SELECT id FROM categories WHERE name = 'Cinema')),
                                                                           ('Popcorn Caramel', 15.00, 1017, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Suc de mere', 15.00, 1017, (SELECT id FROM categories WHERE name = 'Restaurant'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1018, 55.00, '2025-12-15 13:00:00', (SELECT id FROM users WHERE email = 'mihai.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Farmacie Catena' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Picaturi Ochi', 35.00, 1018, (SELECT id FROM categories WHERE name = 'Medicamente')),
                                                                           ('Servetele Umede', 20.00, 1018, (SELECT id FROM categories WHERE name = 'Curatenie'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1019, 38.00, '2025-12-20 22:00:00', (SELECT id FROM users WHERE email = 'cristian.popa@email.com'), (SELECT id FROM locations WHERE store = 'Uber / Bolt' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Cursa seara', 30.00, 1019, (SELECT id FROM categories WHERE name = 'Taxi')),
                                                                           ('Bacsis sofer', 8.00, 1019, (SELECT id FROM categories WHERE name = 'Taxi'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1020, 90.00, '2025-12-25 14:00:00', (SELECT id FROM users WHERE email = 'maria.popa@email.com'), (SELECT id FROM locations WHERE store = 'Restaurant Vivo' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Supa Crema', 25.00, 1020, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Pui cu Gorgonzola', 45.00, 1020, (SELECT id FROM categories WHERE name = 'Restaurant')),
                                                                           ('Espresso', 20.00, 1020, (SELECT id FROM categories WHERE name = 'Cafenea'));

-- IANUARIE 2026
INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1021, 450.00, '2026-01-02 11:00:00', (SELECT id FROM users WHERE email = 'ana.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Kaufland Pacurari' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Detergent Lichid', 100.00, 1021, (SELECT id FROM categories WHERE name = 'Curatenie')),
                                                                           ('Ghiozdan Scoala', 150.00, 1021, (SELECT id FROM categories WHERE name = 'Rechizite')),
                                                                           ('Caiete', 50.00, 1021, (SELECT id FROM categories WHERE name = 'Rechizite')),
                                                                           ('Carne Pui', 100.00, 1021, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Mere', 50.00, 1021, (SELECT id FROM categories WHERE name = 'Supermarket'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1022, 40.00, '2026-01-08 16:30:00', (SELECT id FROM users WHERE email = 'andrei.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Uber / Bolt' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Cursa meditatii', 35.00, 1022, (SELECT id FROM categories WHERE name = 'Taxi')),
                                                                           ('Bacsis', 5.00, 1022, (SELECT id FROM categories WHERE name = 'Taxi'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1023, 35.00, '2026-01-12 10:00:00', (SELECT id FROM users WHERE email = 'sofia.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Starbucks Palas' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Iced Latte', 25.00, 1023, (SELECT id FROM categories WHERE name = 'Cafenea')),
                                                                           ('Cookie cu ciocolata', 10.00, 1023, (SELECT id FROM categories WHERE name = 'Cafenea'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1024, 150.00, '2026-01-18 18:00:00', (SELECT id FROM users WHERE email = 'cristian.popa@email.com'), (SELECT id FROM locations WHERE store = 'Farmacie Catena' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Antibiotic', 90.00, 1024, (SELECT id FROM categories WHERE name = 'Medicamente')),
                                                                           ('Probiotic', 40.00, 1024, (SELECT id FROM categories WHERE name = 'Medicamente')),
                                                                           ('Ibuprofen', 20.00, 1024, (SELECT id FROM categories WHERE name = 'Medicamente'));

INSERT INTO expenses (id, amount, expense_date, user_id, location_id)
VALUES (1025, 110.00, '2026-01-25 19:00:00', (SELECT id FROM users WHERE email = 'radu.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Lidl Iasi' LIMIT 1));
INSERT INTO expense_items (item_name, amount, expense_id, category_id) VALUES
                                                                           ('Cascaval', 40.00, 1025, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Muschi File', 30.00, 1025, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Apa Minerala', 20.00, 1025, (SELECT id FROM categories WHERE name = 'Supermarket')),
                                                                           ('Suc de portocale', 20.00, 1025, (SELECT id FROM categories WHERE name = 'Supermarket'));

-- SOURCE: V40__populate_budgets.sql
-- 1. POPULARE BUGETE (M1 - Stefana)

-- Buget Mancare pentru Familia Popescu (2500 lei pe luna)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 2500.00, '2025-01-01', '2025-12-31',
       (SELECT id FROM families WHERE name = 'Familia Popescu'),
       (SELECT id FROM categories WHERE name = 'Mancare')
    WHERE NOT EXISTS (
    SELECT 1 FROM budgets b
    WHERE b.family_id = (SELECT id FROM families WHERE name = 'Familia Popescu')
    AND b.category_id = (SELECT id FROM categories WHERE name = 'Mancare')
);

-- Buget Transport pentru Familia Ionescu (800 lei pe luna)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 800.00, '2025-01-01', '2025-12-31',
       (SELECT id FROM families WHERE name = 'Familia Ionescu'),
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (
    SELECT 1 FROM budgets b
    WHERE b.family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu')
    AND b.category_id = (SELECT id FROM categories WHERE name = 'Transport')
);

-- Buget Sanatate pentru Familia Popa (1000 lei pe luna)//
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1000.00, '2025-01-01', '2025-12-31',
       (SELECT id FROM families WHERE name = 'Familia Popa'),
       (SELECT id FROM categories WHERE name = 'Sanatate')
    WHERE NOT EXISTS (
    SELECT 1 FROM budgets b
    WHERE b.family_id = (SELECT id FROM families WHERE name = 'Familia Popa')
    AND b.category_id = (SELECT id FROM categories WHERE name = 'Sanatate')
);

-- Buget Divertisment pentru Familia Popescu (600 lei pe luna)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 600.00, '2025-01-01', '2025-12-31',
       (SELECT id FROM families WHERE name = 'Familia Popescu'),
       (SELECT id FROM categories WHERE name = 'Divertisment')
    WHERE NOT EXISTS (
    SELECT 1 FROM budgets b
    WHERE b.family_id = (SELECT id FROM families WHERE name = 'Familia Popescu')
    AND b.category_id = (SELECT id FROM categories WHERE name = 'Divertisment')
);

-- SOURCE: V42__populate_expenses_second.sql (population statements only)



-- 2. POPULARE DESCRIERI PENTRU CHELTUIELILE VECHI

-- Actualizare tabelul 'expenses' pe baza locației
UPDATE expenses SET description = 'Transport local urban' WHERE location_id IN (SELECT id FROM locations WHERE store ILIKE '%Uber%');
UPDATE expenses SET description = 'Iesire in oras' WHERE location_id IN (SELECT id FROM locations WHERE store ILIKE '%Restaurant%' OR store ILIKE '%Starbucks%' OR store ILIKE '%Cinema%');
UPDATE expenses SET description = 'Sanatate si ingrijire' WHERE location_id IN (SELECT id FROM locations WHERE store ILIKE '%Farmacie%');
UPDATE expenses SET description = 'Cumparaturi saptamanale' WHERE description IS NULL;

-- Actualizare tabelul 'expense_items' pe baza categoriei
UPDATE expense_items SET description = 'Alimente si provizii de baza' WHERE category_id = (SELECT id FROM categories WHERE name = 'Supermarket' LIMIT 1);
UPDATE expense_items SET description = 'Produse intretinere locuinta' WHERE category_id = (SELECT id FROM categories WHERE name = 'Curatenie' LIMIT 1);
UPDATE expense_items SET description = 'Cost deplasare' WHERE category_id = (SELECT id FROM categories WHERE name = 'Taxi' LIMIT 1);
UPDATE expense_items SET description = 'Mancare si divertisment' WHERE category_id IN (SELECT id FROM categories WHERE name IN ('Cinema', 'Restaurant', 'Cafenea'));
UPDATE expense_items SET description = 'Tratament si medicatie' WHERE category_id IN (SELECT id FROM categories WHERE name IN ('Medicamente', 'Consultatii'));
UPDATE expense_items SET description = 'Achizitie standard' WHERE description IS NULL;


-- 3. INSERARE DATE NOI PENTRU MARTIE ȘI APRILIE 2026
-- MARTIE 2026

INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1026, 310.00, '2026-03-08 17:30:00', (SELECT id FROM users WHERE email = 'maria.popa@email.com'), (SELECT id FROM locations WHERE store = 'Kaufland Pacurari' LIMIT 1), 'Cumparaturi de weekend la Kaufland');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Fructe bio', 60.00, 1026, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Fructe pentru toata saptamana'),
                                                                                        ('Produse curatenie', 150.00, 1026, (SELECT id FROM categories WHERE name = 'Curatenie'), 'Detergent si solutii de sters praful'),
                                                                                        ('Cosmetice', 100.00, 1026, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Sampon, gel de dus si pasta de dinti');


INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1027, 65.00, '2026-03-15 08:45:00', (SELECT id FROM users WHERE email = 'mihai.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Uber / Bolt' LIMIT 1), 'Uber spre birou dimineata');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Cursa', 50.00, 1027, (SELECT id FROM categories WHERE name = 'Taxi'), 'Trafic infernal din cauza ploii'),
                                                                                        ('Bacsis', 15.00, 1027, (SELECT id FROM categories WHERE name = 'Taxi'), 'Lasat bacsis in aplicatie');


INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1030, 35.00, '2026-03-22 09:15:00', (SELECT id FROM users WHERE email = 'maria.popa@email.com'), (SELECT id FROM locations WHERE store = 'Starbucks Palas' LIMIT 1), 'Cafea de dimineata inainte de sedinta');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Latte mare', 25.00, 1030, (SELECT id FROM categories WHERE name = 'Cafenea'), 'Cafea cu lapte de ovaz'),
                                                                                        ('Croissant', 10.00, 1030, (SELECT id FROM categories WHERE name = 'Cafenea'), 'Mic dejun rapid la pachet');


INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1031, 80.00, '2026-03-28 18:00:00', (SELECT id FROM users WHERE email = 'radu.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Farmacie Catena' LIMIT 1), 'Medicamente pentru alergia de primavara');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Antihistaminice', 45.00, 1031, (SELECT id FROM categories WHERE name = 'Medicamente'), 'Pastile pentru polen'),
                                                                                        ('Picaturi de ochi', 35.00, 1031, (SELECT id FROM categories WHERE name = 'Medicamente'), 'Calmarea iritatiilor');


-- APRILIE 2026
INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1028, 650.00, '2026-04-10 14:00:00', (SELECT id FROM users WHERE email = 'ana.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Lidl Iasi' LIMIT 1), 'Cumparaturi mari pentru masa de Paste');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Carne de miel', 250.00, 1028, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Carne proaspata pentru friptura'),
                                                                                        ('Oua si vopsea', 50.00, 1028, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Oua albe si vopsea rosie'),
                                                                                        ('Cozonac ambalat', 150.00, 1028, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Doi cozonaci cu nuca si cacao'),
                                                                                        ('Bauturi si vin', 200.00, 1028, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Vin rosu si sucuri pentru musafiri');


INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1029, 180.00, '2026-04-20 19:30:00', (SELECT id FROM users WHERE email = 'cristian.popa@email.com'), (SELECT id FROM locations WHERE store = 'Restaurant Vivo' LIMIT 1), 'Cina in oras dupa munca');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Burger vita dublu', 90.00, 1029, (SELECT id FROM categories WHERE name = 'Restaurant'), 'Pofta de burger cu cartofi prajiti'),
                                                                                        ('Bauturi artizanale', 60.00, 1029, (SELECT id FROM categories WHERE name = 'Restaurant'), 'Doua beri reci la draft'),
                                                                                        ('Desert', 30.00, 1029, (SELECT id FROM categories WHERE name = 'Restaurant'), 'Un cheesecake impartit la doi');


INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1032, 105.00, '2026-04-04 19:00:00', (SELECT id FROM users WHERE email = 'andrei.ionescu@email.com'), (SELECT id FROM locations WHERE store = 'Cinema City' LIMIT 1), 'Iesit la un film nou in weekend');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Bilete premiera', 60.00, 1032, (SELECT id FROM categories WHERE name = 'Cinema'), 'Doua bilete in randul din mijloc'),
                                                                                        ('Meniu popcorn si suc', 45.00, 1032, (SELECT id FROM categories WHERE name = 'Restaurant'), 'Popcorn mare si cola pentru film');


INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1033, 100.00, '2026-04-25 10:30:00', (SELECT id FROM users WHERE email = 'ana.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Kaufland Pacurari' LIMIT 1), 'Completare provizii dupa sarbatori');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
                                                                                        ('Apa plata bax', 20.00, 1033, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Doua baxuri de apa la 2 litri'),
                                                                                        ('Legume proaspete', 35.00, 1033, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Rosii, castraveti si salata pentru dieta'),
                                                                                        ('Paine si mezeluri', 45.00, 1033, (SELECT id FROM categories WHERE name = 'Supermarket'), 'Mic dejun pentru restul saptamanii');


INSERT INTO expenses (id, amount, expense_date, user_id, location_id, description)
VALUES (1034, 45.00, '2026-04-28 01:15:00', (SELECT id FROM users WHERE email = 'sofia.popescu@email.com'), (SELECT id FROM locations WHERE store = 'Uber / Bolt' LIMIT 1), 'Intoarcere acasa de la petrecere');

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description) VALUES
    ('Cursa tarif de noapte', 45.00, 1034, (SELECT id FROM categories WHERE name = 'Taxi'), 'Tarif dinamic aplicat dupa miezul noptii');

-- SOURCE: V43__more_budget_data.sql
-- 1. FAMILIA POPESCU (Gama completa de bugete)
-- Utilitati
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1200.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Utilitati')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popescu') AND category_id = (SELECT id FROM categories WHERE name = 'Utilitati'));
-- Igiena
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 350.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Igiena')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popescu') AND category_id = (SELECT id FROM categories WHERE name = 'Igiena'));

-- Vacanta de vara (Divertisment)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 5000.00, '2025-06-01', '2025-08-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Divertisment')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE amount = 5000.00 AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));

-- Cadouri Craciun
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1500.00, '2025-12-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Cadouri')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Cadouri') AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));

-- Hobby
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 400.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Hobby')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Hobby') AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));


-- 2. FAMILIA IONESCU (Focus Educatie si Casa)
-- Educatie
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 2000.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Educatie')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu') AND category_id = (SELECT id FROM categories WHERE name = 'Educatie'));

-- Mancare
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 2500.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Mancare')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu') AND category_id = (SELECT id FROM categories WHERE name = 'Mancare'));

-- Imbracaminte
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 800.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Imbracaminte')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu') AND category_id = (SELECT id FROM categories WHERE name = 'Imbracaminte'));

-- Casa (Reparatii/Intretinere)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1000.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Casa')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Casa') AND family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu'));

-- Abonamente (Netflix, Sala, etc)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 250.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Abonamente')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Abonamente') AND family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu'));

-- 3. FAMILIA POPA (Focus Sanatate si Economii)
-- Sanatate
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1200.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Sanatate')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popa') AND category_id = (SELECT id FROM categories WHERE name = 'Sanatate'));

-- Transport
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 600.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popa') AND category_id = (SELECT id FROM categories WHERE name = 'Transport'));

-- Mancare
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 2100.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Mancare')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popa') AND category_id = (SELECT id FROM categories WHERE name = 'Mancare'));

-- Economii (Fond de urgenta)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 3000.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Economii')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Economii') AND family_id = (SELECT id FROM families WHERE name = 'Familia Popa'));

-- Diverse/Altele
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 500.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Altele')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Altele') AND family_id = (SELECT id FROM families WHERE name = 'Familia Popa'));


-- 4. BUGETE PE TERMEN SCURT (Saptamanale - Pentru Analytics)
-- Saptamana 1 Ianuarie (Mancare speciala)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 400.00, '2025-01-01', '2025-01-07', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Mancare')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE amount = 400.00 AND start_date = '2025-01-01' AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));

-- Saptamana 2 Ianuarie (Mancare speciala)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 400.00, '2025-01-08', '2025-01-14', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Mancare')
    WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE amount = 400.00 AND start_date = '2025-01-08' AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));

-- SOURCE: V46__populare2.sql


INSERT INTO users (name, email, password_h, created_at)
SELECT 'Ioana Dumitrescu', 'ioana.dumitrescu@email.com', '$2a$10$dummyHashIoanaDumitrescu0000000000000000000000000000', '2024-04-12'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'ioana.dumitrescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Alexandru Dumitrescu', 'alexandru.dumitrescu@email.com', '$2a$10$dummyHashAlexandruDumitrescu000000000000000000000', '2024-04-12'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'alexandru.dumitrescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Matei Dumitrescu', 'matei.dumitrescu@email.com', '$2a$10$dummyHashMateiDumitrescu000000000000000000000000000', '2024-04-12'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'matei.dumitrescu@email.com');




INSERT INTO users (name, email, password_h, created_at)
SELECT 'Laura Marinescu', 'laura.marinescu@email.com', '$2a$10$dummyHashLauraMarinescu0000000000000000000000000000', '2024-05-08'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'laura.marinescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Vlad Marinescu', 'vlad.marinescu@email.com', '$2a$10$dummyHashVladMarinescu00000000000000000000000000000', '2024-05-08'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'vlad.marinescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Emma Marinescu', 'emma.marinescu@email.com', '$2a$10$dummyHashEmmaMarinescu00000000000000000000000000000', '2024-05-08'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'emma.marinescu@email.com');




INSERT INTO families (name, created_at)
SELECT 'Familia Dumitrescu', '2024-04-12'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Dumitrescu');

INSERT INTO families (name, created_at)
SELECT 'Familia Marinescu', '2024-05-08'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Marinescu');




INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'ioana.dumitrescu@email.com' AND f.name = 'Familia Dumitrescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'alexandru.dumitrescu@email.com' AND f.name = 'Familia Dumitrescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'matei.dumitrescu@email.com' AND f.name = 'Familia Dumitrescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);




INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'laura.marinescu@email.com' AND f.name = 'Familia Marinescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'vlad.marinescu@email.com' AND f.name = 'Familia Marinescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'emma.marinescu@email.com' AND f.name = 'Familia Marinescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);


INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Haine', 'Imbracaminte si accesorii', true,
       (SELECT id FROM categories WHERE name = 'Shopping')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Haine');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Electronice', 'Telefoane, laptopuri si accesorii electronice', true,
       (SELECT id FROM categories WHERE name = 'Shopping')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Electronice');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Ingrijire personala', 'Produse de igiena si cosmetice', true,
       (SELECT id FROM categories WHERE name = 'Shopping')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ingrijire personala');


INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Retragere bancomat', 'Retrageri de numerar de la bancomat', true,
       (SELECT id FROM categories WHERE name = 'Numerar')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Retragere bancomat');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Plata cash', 'Plati efectuate in numerar', true,
       (SELECT id FROM categories WHERE name = 'Numerar')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Plata cash');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Neprevazute', 'Cheltuieli neprevazute', true,
       (SELECT id FROM categories WHERE name = 'Altele')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Neprevazute');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Diverse', 'Cheltuieli diverse', true,
       (SELECT id FROM categories WHERE name = 'Altele')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Diverse');


INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Mobila', 'Mobila si decoratiuni pentru locuinta', true,
       (SELECT id FROM categories WHERE name = 'Pentru casa')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mobila');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Reparatii casa', 'Reparatii si intretinere locuinta', true,
       (SELECT id FROM categories WHERE name = 'Pentru casa')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Reparatii casa');


INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Internet', 'Abonament internet', true,
       (SELECT id FROM categories WHERE name = 'Servicii')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Internet');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Asigurari', 'Asigurari si polite lunare', true,
       (SELECT id FROM categories WHERE name = 'Servicii')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Asigurari');


INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Parcare', 'Taxe de parcare', true,
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Parcare');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Service auto', 'Reparatii si intretinere auto', true,
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Service auto');

-- SOURCE: V47__populare3.sql
INSERT INTO users (name, email, password_h, created_at)
SELECT 'Diana Stan', 'diana.stan@email.com', '$2a$10$dummyHashDianaStan000000000000000000000000000000000000', '2024-06-03'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'diana.stan@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'George Stan', 'george.stan@email.com', '$2a$10$dummyHashGeorgeStan00000000000000000000000000000000000', '2024-06-03'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'george.stan@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Mara Stan', 'mara.stan@email.com', '$2a$10$dummyHashMaraStan0000000000000000000000000000000000000', '2024-06-03'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'mara.stan@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Alina Pavel', 'alina.pavel@email.com', '$2a$10$dummyHashAlinaPavel00000000000000000000000000000000000', '2024-07-18'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'alina.pavel@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Sorin Pavel', 'sorin.pavel@email.com', '$2a$10$dummyHashSorinPavel00000000000000000000000000000000000', '2024-07-18'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'sorin.pavel@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Tudor Pavel', 'tudor.pavel@email.com', '$2a$10$dummyHashTudorPavel00000000000000000000000000000000000', '2024-07-18'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'tudor.pavel@email.com');

INSERT INTO families (name, created_at)
SELECT 'Familia Stan', '2024-06-03'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Stan');

INSERT INTO families (name, created_at)
SELECT 'Familia Pavel', '2024-07-18'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Pavel');

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'diana.stan@email.com' AND f.name = 'Familia Stan'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'george.stan@email.com' AND f.name = 'Familia Stan'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'mara.stan@email.com' AND f.name = 'Familia Stan'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'alina.pavel@email.com' AND f.name = 'Familia Pavel'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'sorin.pavel@email.com' AND f.name = 'Familia Pavel'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'tudor.pavel@email.com' AND f.name = 'Familia Pavel'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

-- SOURCE: V48__populare4.sql
INSERT INTO users (name, email, password_h, created_at)
SELECT 'Irina Vasilescu', 'irina.vasilescu@email.com', '$2a$10$dummyHashIrinaVasilescu000000000000000000000000000', '2024-08-09'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'irina.vasilescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Bogdan Vasilescu', 'bogdan.vasilescu@email.com', '$2a$10$dummyHashBogdanVasilescu00000000000000000000000000', '2024-08-09'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'bogdan.vasilescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Sara Vasilescu', 'sara.vasilescu@email.com', '$2a$10$dummyHashSaraVasilescu0000000000000000000000000000', '2024-08-09'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'sara.vasilescu@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Raluca Enache', 'raluca.enache@email.com', '$2a$10$dummyHashRalucaEnache00000000000000000000000000000', '2024-09-14'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'raluca.enache@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'Florin Enache', 'florin.enache@email.com', '$2a$10$dummyHashFlorinEnache00000000000000000000000000000', '2024-09-14'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'florin.enache@email.com');

INSERT INTO users (name, email, password_h, created_at)
SELECT 'David Enache', 'david.enache@email.com', '$2a$10$dummyHashDavidEnache000000000000000000000000000000', '2024-09-14'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'david.enache@email.com');

INSERT INTO families (name, created_at)
SELECT 'Familia Vasilescu', '2024-08-09'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Vasilescu');

INSERT INTO families (name, created_at)
SELECT 'Familia Enache', '2024-09-14'
    WHERE NOT EXISTS (SELECT 1 FROM families WHERE name = 'Familia Enache');

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'irina.vasilescu@email.com' AND f.name = 'Familia Vasilescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'bogdan.vasilescu@email.com' AND f.name = 'Familia Vasilescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'sara.vasilescu@email.com' AND f.name = 'Familia Vasilescu'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'raluca.enache@email.com' AND f.name = 'Familia Enache'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'parent'
FROM users u, families f
WHERE u.email = 'florin.enache@email.com' AND f.name = 'Familia Enache'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, 'child'
FROM users u, families f
WHERE u.email = 'david.enache@email.com' AND f.name = 'Familia Enache'
  AND NOT EXISTS (
    SELECT 1 FROM family_members fm
    WHERE fm.user_id = u.id AND fm.family_id = f.id
);

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Benzinarie', 'Cheltuieli in benzinarii', true,
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Benzinarie');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Rovinieta', 'Taxe de drum si rovinieta', true,
       (SELECT id FROM categories WHERE name = 'Transport')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Rovinieta');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Lactate', 'Lapte, branza, iaurt si alte lactate', true,
       (SELECT id FROM categories WHERE name = 'Mâncare')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Lactate');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Fructe si legume', 'Fructe, legume si produse proaspete', true,
       (SELECT id FROM categories WHERE name = 'Mâncare')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Fructe si legume');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Jucarii', 'Jucarii si produse pentru copii', true,
       (SELECT id FROM categories WHERE name = 'Shopping')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Jucarii');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Carti', 'Carti si materiale de lectura', true,
       (SELECT id FROM categories WHERE name = 'Shopping')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Carti');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Gradinita', 'Taxe si cheltuieli pentru gradinita', true,
       (SELECT id FROM categories WHERE name = 'Educatie')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Gradinita');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Activitati extrascolare', 'Activitati si cursuri extrascolare', true,
       (SELECT id FROM categories WHERE name = 'Educatie')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Activitati extrascolare');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Produse menaj', 'Produse pentru curatenie si intretinere', true,
       (SELECT id FROM categories WHERE name = 'Pentru casa')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Produse menaj');

INSERT INTO categories (name, description, is_active, parent_id)
SELECT 'Decoratiuni', 'Decoratiuni si accesorii pentru casa', true,
       (SELECT id FROM categories WHERE name = 'Pentru casa')
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Decoratiuni');

SELECT setval(pg_get_serial_sequence('families', 'id'), COALESCE((SELECT MAX(id) FROM families), 1), true);
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('family_members', 'id'), COALESCE((SELECT MAX(id) FROM family_members), 1), true);
SELECT setval(pg_get_serial_sequence('categories', 'id'), COALESCE((SELECT MAX(id) FROM categories), 1), true);
SELECT setval(pg_get_serial_sequence('locations', 'id'), COALESCE((SELECT MAX(id) FROM locations), 1), true);
SELECT setval(pg_get_serial_sequence('expenses', 'id'), COALESCE((SELECT MAX(id) FROM expenses), 1), true);
SELECT setval(pg_get_serial_sequence('expense_items', 'id'), COALESCE((SELECT MAX(id) FROM expense_items), 1), true);
SELECT setval(pg_get_serial_sequence('budgets', 'id'), COALESCE((SELECT MAX(id) FROM budgets), 1), true);

COMMIT;