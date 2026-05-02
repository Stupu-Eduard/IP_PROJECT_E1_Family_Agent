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