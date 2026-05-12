
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS description VARCHAR(255);
ALTER TABLE expense_items ADD COLUMN IF NOT EXISTS description VARCHAR(255);


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