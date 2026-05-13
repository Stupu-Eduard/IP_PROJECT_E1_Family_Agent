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

INSERT INTO users (name, email, password_h, created_at) VALUES
                                                            ('Ion Ionescu', 'ion@email.com', 'parola_securizata_123', '2024-01-01'),
                                                            ('Ana Popescu', 'ana.popescu@email.com', '$2a$10$dummyHashAnaPopescu000000000000000000000000000000000', '2024-01-10'),
                                                            ('Mihai Popescu', 'mihai.popescu@email.com', '$2a$10$dummyHashMihaiPopescu00000000000000000000000000000000', '2024-01-10'),
                                                            ('Sofia Popescu', 'sofia.popescu@email.com', '$2a$10$dummyHashSofiaPopescu00000000000000000000000000000000', '2024-01-10'),
                                                            ('Radu Ionescu', 'radu.ionescu@email.com', '$2a$10$dummyHashRaduIonescu000000000000000000000000000000000', '2024-02-05'),
                                                            ('Elena Ionescu', 'elena.ionescu@email.com', '$2a$10$dummyHashElenaIonescu00000000000000000000000000000000', '2024-02-05'),
                                                            ('Andrei Ionescu', 'andrei.ionescu@email.com', '$2a$10$dummyHashAndreiIonescu0000000000000000000000000000000', '2024-02-05'),
                                                            ('Maria Popa', 'maria.popa@email.com', '$2a$10$dummyHashMariaPopa000000000000000000000000000000000000', '2024-03-15'),
                                                            ('Cristian Popa', 'cristian.popa@email.com', '$2a$10$dummyHashCristianPopa00000000000000000000000000000000', '2024-03-15'),
                                                            ('Ioana Dumitrescu', 'ioana.dumitrescu@email.com', '$2a$10$dummyHashIoanaDumitrescu0000000000000000000000000000', '2024-04-12'),
                                                            ('Alexandru Dumitrescu', 'alexandru.dumitrescu@email.com', '$2a$10$dummyHashAlexandruDumitrescu000000000000000000000', '2024-04-12'),
                                                            ('Matei Dumitrescu', 'matei.dumitrescu@email.com', '$2a$10$dummyHashMateiDumitrescu000000000000000000000000000', '2024-04-12'),
                                                            ('Laura Marinescu', 'laura.marinescu@email.com', '$2a$10$dummyHashLauraMarinescu0000000000000000000000000000', '2024-05-08'),
                                                            ('Vlad Marinescu', 'vlad.marinescu@email.com', '$2a$10$dummyHashVladMarinescu00000000000000000000000000000', '2024-05-08'),
                                                            ('Emma Marinescu', 'emma.marinescu@email.com', '$2a$10$dummyHashEmmaMarinescu00000000000000000000000000000', '2024-05-08'),
                                                            ('Diana Stan', 'diana.stan@email.com', '$2a$10$dummyHashDianaStan000000000000000000000000000000000000', '2024-06-03'),
                                                            ('George Stan', 'george.stan@email.com', '$2a$10$dummyHashGeorgeStan00000000000000000000000000000000000', '2024-06-03'),
                                                            ('Mara Stan', 'mara.stan@email.com', '$2a$10$dummyHashMaraStan0000000000000000000000000000000000000', '2024-06-03'),
                                                            ('Alina Pavel', 'alina.pavel@email.com', '$2a$10$dummyHashAlinaPavel00000000000000000000000000000000000', '2024-07-18'),
                                                            ('Sorin Pavel', 'sorin.pavel@email.com', '$2a$10$dummyHashSorinPavel00000000000000000000000000000000000', '2024-07-18'),
                                                            ('Tudor Pavel', 'tudor.pavel@email.com', '$2a$10$dummyHashTudorPavel00000000000000000000000000000000000', '2024-07-18'),
                                                            ('Irina Vasilescu', 'irina.vasilescu@email.com', '$2a$10$dummyHashIrinaVasilescu000000000000000000000000000', '2024-08-09'),
                                                            ('Bogdan Vasilescu', 'bogdan.vasilescu@email.com', '$2a$10$dummyHashBogdanVasilescu00000000000000000000000000', '2024-08-09'),
                                                            ('Sara Vasilescu', 'sara.vasilescu@email.com', '$2a$10$dummyHashSaraVasilescu0000000000000000000000000000', '2024-08-09'),
                                                            ('Raluca Enache', 'raluca.enache@email.com', '$2a$10$dummyHashRalucaEnache00000000000000000000000000000', '2024-09-14'),
                                                            ('Florin Enache', 'florin.enache@email.com', '$2a$10$dummyHashFlorinEnache00000000000000000000000000000', '2024-09-14'),
                                                            ('David Enache', 'david.enache@email.com', '$2a$10$dummyHashDavidEnache000000000000000000000000000000', '2024-09-14');

INSERT INTO families (name, created_at) VALUES
                                            ('Familia Popescu', '2024-01-10'),
                                            ('Familia Ionescu', '2024-02-05'),
                                            ('Familia Popa', '2024-03-15'),
                                            ('Familia Dumitrescu', '2024-04-12'),
                                            ('Familia Marinescu', '2024-05-08'),
                                            ('Familia Stan', '2024-06-03'),
                                            ('Familia Pavel', '2024-07-18'),
                                            ('Familia Vasilescu', '2024-08-09'),
                                            ('Familia Enache', '2024-09-14');

INSERT INTO family_members (user_id, family_id, role)
SELECT u.id, f.id, v.role
FROM (VALUES
          ('ion@email.com', 'Familia Ionescu', 'ADMIN'),
          ('ana.popescu@email.com', 'Familia Popescu', 'parent'),
          ('mihai.popescu@email.com', 'Familia Popescu', 'parent'),
          ('sofia.popescu@email.com', 'Familia Popescu', 'child'),
          ('radu.ionescu@email.com', 'Familia Ionescu', 'parent'),
          ('elena.ionescu@email.com', 'Familia Ionescu', 'child'),
          ('andrei.ionescu@email.com', 'Familia Ionescu', 'child'),
          ('maria.popa@email.com', 'Familia Popa', 'parent'),
          ('cristian.popa@email.com', 'Familia Popa', 'parent'),
          ('ioana.dumitrescu@email.com', 'Familia Dumitrescu', 'parent'),
          ('alexandru.dumitrescu@email.com', 'Familia Dumitrescu', 'parent'),
          ('matei.dumitrescu@email.com', 'Familia Dumitrescu', 'child'),
          ('laura.marinescu@email.com', 'Familia Marinescu', 'parent'),
          ('vlad.marinescu@email.com', 'Familia Marinescu', 'parent'),
          ('emma.marinescu@email.com', 'Familia Marinescu', 'child'),
          ('diana.stan@email.com', 'Familia Stan', 'parent'),
          ('george.stan@email.com', 'Familia Stan', 'parent'),
          ('mara.stan@email.com', 'Familia Stan', 'child'),
          ('alina.pavel@email.com', 'Familia Pavel', 'parent'),
          ('sorin.pavel@email.com', 'Familia Pavel', 'parent'),
          ('tudor.pavel@email.com', 'Familia Pavel', 'child'),
          ('irina.vasilescu@email.com', 'Familia Vasilescu', 'parent'),
          ('bogdan.vasilescu@email.com', 'Familia Vasilescu', 'parent'),
          ('sara.vasilescu@email.com', 'Familia Vasilescu', 'child'),
          ('raluca.enache@email.com', 'Familia Enache', 'parent'),
          ('florin.enache@email.com', 'Familia Enache', 'parent'),
          ('david.enache@email.com', 'Familia Enache', 'child')
     ) AS v(email, family_name, role)
         JOIN users u ON u.email = v.email
         JOIN families f ON f.name = v.family_name;

INSERT INTO categories (name, description, is_active) VALUES
                                                          ('Mâncare', 'Cheltuieli legate de alimente si masa', true),
                                                          ('Transport', 'Cheltuieli legate de deplasare', true),
                                                          ('Sănătate', 'Cheltuieli medicale si de sanatate', true),
                                                          ('Divertisment', 'Cheltuieli de timp liber si activitati', true),
                                                          ('Educatie', 'Cheltuieli scolare si de formare', true),
                                                          ('Shopping', 'Cumparaturi diverse', true),
                                                          ('Numerar', 'Retrageri si plati in numerar', true),
                                                          ('Servicii', 'Abonamente si servicii platite', true),
                                                          ('Pentru casa', 'Cheltuieli pentru locuinta', true),
                                                          ('Altele', 'Cheltuieli diverse neclasificate', true);

INSERT INTO categories (name, description, is_active, parent_id)
SELECT v.name, v.description, true, p.id
FROM (VALUES
          ('Supermarket', 'Cumparaturi din supermarket', 'Mâncare'),
          ('Restaurant', 'Masa la restaurant sau fast-food', 'Mâncare'),
          ('Cafenea', 'Cafea si bauturi', 'Mâncare'),
          ('Carburant', 'Benzina, motorina, GPL', 'Transport'),
          ('Taxi', 'Taxi si ride-sharing (Uber, Bolt)', 'Transport'),
          ('Transport public', 'Bilete si abonamente STB, CFR', 'Transport'),
          ('Medicamente', 'Medicamente si suplimente', 'Sănătate'),
          ('Consultatii', 'Consultatii si analize medicale', 'Sănătate'),
          ('Rechizite', 'Rechizite si materiale scolare', 'Educatie'),
          ('Cursuri', 'Cursuri si training-uri', 'Educatie'),
          ('Streaming', 'Netflix, Spotify, abonamente online', 'Divertisment'),
          ('Cinema', 'Bilete cinema si teatru', 'Divertisment'),
          ('Utilitati', 'Curent, apa, gaz, internet', 'Servicii'),
          ('Telefonie', 'Abonament telefon mobil', 'Servicii'),
          ('Chirie', 'Chirie si rate', 'Pentru casa'),
          ('Curatenie', 'Produse de curatenie si uz casnic', 'Pentru casa'),
          ('Haine', 'Imbracaminte si accesorii', 'Shopping'),
          ('Electronice', 'Telefoane, laptopuri si accesorii electronice', 'Shopping'),
          ('Ingrijire personala', 'Produse de igiena si cosmetice', 'Shopping'),
          ('Retragere bancomat', 'Retrageri de numerar de la bancomat', 'Numerar'),
          ('Plata cash', 'Plati efectuate in numerar', 'Numerar'),
          ('Neprevazute', 'Cheltuieli neprevazute', 'Altele'),
          ('Diverse', 'Cheltuieli diverse', 'Altele'),
          ('Mobila', 'Mobila si decoratiuni pentru locuinta', 'Pentru casa'),
          ('Reparatii casa', 'Reparatii si intretinere locuinta', 'Pentru casa'),
          ('Internet', 'Abonament internet', 'Servicii'),
          ('Asigurari', 'Asigurari si polite lunare', 'Servicii'),
          ('Parcare', 'Taxe de parcare', 'Transport'),
          ('Service auto', 'Reparatii si intretinere auto', 'Transport'),
          ('Benzinarie', 'Cheltuieli in benzinarii', 'Transport'),
          ('Rovinieta', 'Taxe de drum si rovinieta', 'Transport'),
          ('Lactate', 'Lapte, branza, iaurt si alte lactate', 'Mâncare'),
          ('Fructe si legume', 'Fructe, legume si produse proaspete', 'Mâncare'),
          ('Jucarii', 'Jucarii si produse pentru copii', 'Shopping'),
          ('Carti', 'Carti si materiale de lectura', 'Shopping'),
          ('Gradinita', 'Taxe si cheltuieli pentru gradinita', 'Educatie'),
          ('Activitati extrascolare', 'Activitati si cursuri extrascolare', 'Educatie'),
          ('Produse menaj', 'Produse pentru curatenie si intretinere', 'Pentru casa'),
          ('Decoratiuni', 'Decoratiuni si accesorii pentru casa', 'Pentru casa'),
          ('Igiena', 'Produse de igiena si ingrijire', 'Shopping'),
          ('Cadouri', 'Cadouri si ocazii speciale', 'Altele'),
          ('Hobby', 'Activitati de relaxare si pasiuni', 'Divertisment'),
          ('Imbracaminte', 'Haine si accesorii', 'Shopping'),
          ('Casa', 'Reparatii si intretinere locuinta', 'Pentru casa'),
          ('Abonamente', 'Abonamente recurente', 'Servicii'),
          ('Economii', 'Fonduri puse deoparte', 'Altele')
     ) AS v(name, description, parent_name)
         JOIN categories p ON p.name = v.parent_name;

INSERT INTO locations (store, address, city, country, latitude, longitude) VALUES
                                                                               ('Kaufland', 'Soseaua Pantelimon 244, Sector 2', 'Bucuresti', 'Romania', NULL, NULL),
                                                                               ('Benzinăria OMV', 'Soseaua Mihai Bravu 254, Sector 3', 'Bucuresti', 'Romania', NULL, NULL),
                                                                               ('Mega Image', 'Bulevardul Decebal 6, Sector 3', 'Bucuresti', 'Romania', NULL, NULL),
                                                                               ('Lidl Iasi', NULL, 'Iasi', 'Romania', 47.1585, 27.5852),
                                                                               ('Kaufland Pacurari', NULL, 'Iasi', 'Romania', 47.172, 27.55),
                                                                               ('Uber / Bolt', NULL, 'Iasi', 'Romania', 47.161, 27.592),
                                                                               ('Farmacie Catena', NULL, 'Iasi', 'Romania', 47.165, 27.58),
                                                                               ('Restaurant Vivo', NULL, 'Iasi', 'Romania', 47.155, 27.6),
                                                                               ('Starbucks Palas', NULL, 'Iasi', 'Romania', 47.1565, 27.5875),
                                                                               ('Cinema City', NULL, 'Iasi', 'Romania', 47.154, 27.589);

INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT v.amount, v.start_date::date, v.end_date::date, f.id, c.id
FROM (VALUES
          (2500.00, '2025-01-01', '2025-12-31', 'Familia Popescu', 'Mâncare'),
          (800.00, '2025-01-01', '2025-12-31', 'Familia Ionescu', 'Transport'),
          (1000.00, '2025-01-01', '2025-12-31', 'Familia Popa', 'Sănătate'),
          (600.00, '2025-01-01', '2025-12-31', 'Familia Popescu', 'Divertisment'),
          (1200.00, '2025-01-01', '2025-12-31', 'Familia Popescu', 'Utilitati'),
          (350.00, '2025-01-01', '2025-12-31', 'Familia Popescu', 'Igiena'),
          (5000.00, '2025-06-01', '2025-08-31', 'Familia Popescu', 'Divertisment'),
          (1500.00, '2025-12-01', '2025-12-31', 'Familia Popescu', 'Cadouri'),
          (400.00, '2025-01-01', '2025-12-31', 'Familia Popescu', 'Hobby'),
          (2000.00, '2025-01-01', '2025-12-31', 'Familia Ionescu', 'Educatie'),
          (2500.00, '2025-01-01', '2025-12-31', 'Familia Ionescu', 'Mâncare'),
          (800.00, '2025-01-01', '2025-12-31', 'Familia Ionescu', 'Imbracaminte'),
          (1000.00, '2025-01-01', '2025-12-31', 'Familia Ionescu', 'Casa'),
          (250.00, '2025-01-01', '2025-12-31', 'Familia Ionescu', 'Abonamente'),
          (1200.00, '2025-01-01', '2025-12-31', 'Familia Popa', 'Sănătate'),
          (600.00, '2025-01-01', '2025-12-31', 'Familia Popa', 'Transport'),
          (2100.00, '2025-01-01', '2025-12-31', 'Familia Popa', 'Mâncare'),
          (3000.00, '2025-01-01', '2025-12-31', 'Familia Popa', 'Economii'),
          (500.00, '2025-01-01', '2025-12-31', 'Familia Popa', 'Altele'),
          (400.00, '2025-01-01', '2025-01-07', 'Familia Popescu', 'Mâncare'),
          (400.00, '2025-01-08', '2025-01-14', 'Familia Popescu', 'Mâncare')
     ) AS v(amount, start_date, end_date, family_name, category_name)
         JOIN families f ON f.name = v.family_name
         JOIN categories c ON c.name = v.category_name;

INSERT INTO expenses (id, amount, expense_date, currency, source_type, user_id, family_id, category_id, location_id, description)
SELECT v.id, v.amount, v.expense_date::timestamp, v.currency, v.source_type,
       u.id, fm.family_id, c.id, l.id, v.description
FROM (VALUES
          (1, 150.00, CURRENT_TIMESTAMP, 'RON', 'MANUAL', 'ion@email.com', 'Mâncare', 'Kaufland', 'Cumpărături supermarket'),
          (2, 85.50, '2026-03-27 10:00:00', 'RON', 'MANUAL', 'ion@email.com', 'Mâncare', 'Mega Image', 'Cumparaturi mic dejun'),
          (3, 150.50, CURRENT_TIMESTAMP, 'RON', 'MANUAL', 'ion@email.com', 'Mâncare', 'Kaufland', 'Cumpărături săptămânale'),
          (4, 45.00, CURRENT_TIMESTAMP, 'RON', 'MANUAL', 'ion@email.com', 'Transport', 'Benzinăria OMV', 'Benzină'),
          (1001, 185.00, '2025-09-15 10:30:00', 'RON', 'MANUAL', 'ana.popescu@email.com', 'Supermarket', 'Lidl Iasi', 'Cumparaturi saptamanale'),
          (1002, 25.00, '2025-09-20 18:00:00', 'RON', 'MANUAL', 'sofia.popescu@email.com', 'Taxi', 'Uber / Bolt', 'Transport local urban'),
          (1003, 250.00, '2025-09-22 14:00:00', 'RON', 'MANUAL', 'maria.popa@email.com', 'Supermarket', 'Lidl Iasi', 'Cumparaturi saptamanale'),
          (1004, 120.00, '2025-09-28 19:30:00', 'RON', 'MANUAL', 'radu.ionescu@email.com', 'Cinema', 'Cinema City', 'Iesire in oras'),
          (1005, 150.00, '2025-09-30 20:00:00', 'RON', 'MANUAL', 'mihai.popescu@email.com', 'Restaurant', 'Restaurant Vivo', 'Iesire in oras'),
          (1006, 320.00, '2025-10-05 17:45:00', 'RON', 'MANUAL', 'radu.ionescu@email.com', 'Supermarket', 'Kaufland Pacurari', 'Cumparaturi saptamanale'),
          (1007, 45.00, '2025-10-14 15:20:00', 'RON', 'MANUAL', 'elena.ionescu@email.com', 'Cafenea', 'Starbucks Palas', 'Iesire in oras'),
          (1008, 180.00, '2025-10-18 10:00:00', 'RON', 'MANUAL', 'cristian.popa@email.com', 'Supermarket', 'Kaufland Pacurari', 'Cumparaturi saptamanale'),
          (1009, 22.00, '2025-10-22 14:00:00', 'RON', 'MANUAL', 'elena.ionescu@email.com', 'Taxi', 'Uber / Bolt', 'Transport local urban'),
          (1010, 85.00, '2025-10-28 09:30:00', 'RON', 'MANUAL', 'ana.popescu@email.com', 'Medicamente', 'Farmacie Catena', 'Sanatate si ingrijire'),
          (1011, 210.00, '2025-11-10 11:00:00', 'RON', 'MANUAL', 'maria.popa@email.com', 'Consultatii', 'Farmacie Catena', 'Sanatate si ingrijire'),
          (1012, 135.00, '2025-11-15 19:30:00', 'RON', 'MANUAL', 'cristian.popa@email.com', 'Restaurant', 'Restaurant Vivo', 'Iesire in oras'),
          (1013, 200.00, '2025-11-18 20:00:00', 'RON', 'MANUAL', 'ana.popescu@email.com', 'Restaurant', 'Restaurant Vivo', 'Iesire in oras'),
          (1014, 45.00, '2025-11-22 17:00:00', 'RON', 'MANUAL', 'andrei.ionescu@email.com', 'Cinema', 'Cinema City', 'Iesire in oras'),
          (1015, 60.00, '2025-11-28 10:00:00', 'RON', 'MANUAL', 'maria.popa@email.com', 'Cafenea', 'Starbucks Palas', 'Iesire in oras'),
          (1016, 30.00, '2025-12-02 08:30:00', 'RON', 'MANUAL', 'mihai.popescu@email.com', 'Taxi', 'Uber / Bolt', 'Transport local urban'),
          (1017, 70.00, '2025-12-10 20:00:00', 'RON', 'MANUAL', 'sofia.popescu@email.com', 'Cinema', 'Cinema City', 'Iesire in oras'),
          (1018, 55.00, '2025-12-15 13:00:00', 'RON', 'MANUAL', 'mihai.popescu@email.com', 'Medicamente', 'Farmacie Catena', 'Sanatate si ingrijire'),
          (1019, 38.00, '2025-12-20 22:00:00', 'RON', 'MANUAL', 'cristian.popa@email.com', 'Taxi', 'Uber / Bolt', 'Transport local urban'),
          (1020, 90.00, '2025-12-25 14:00:00', 'RON', 'MANUAL', 'maria.popa@email.com', 'Restaurant', 'Restaurant Vivo', 'Iesire in oras'),
          (1021, 450.00, '2026-01-02 11:00:00', 'RON', 'MANUAL', 'ana.popescu@email.com', 'Curatenie', 'Kaufland Pacurari', 'Cumparaturi saptamanale'),
          (1022, 40.00, '2026-01-08 16:30:00', 'RON', 'MANUAL', 'andrei.ionescu@email.com', 'Taxi', 'Uber / Bolt', 'Transport local urban'),
          (1023, 35.00, '2026-01-12 10:00:00', 'RON', 'MANUAL', 'sofia.popescu@email.com', 'Cafenea', 'Starbucks Palas', 'Iesire in oras'),
          (1024, 150.00, '2026-01-18 18:00:00', 'RON', 'MANUAL', 'cristian.popa@email.com', 'Medicamente', 'Farmacie Catena', 'Sanatate si ingrijire'),
          (1025, 110.00, '2026-01-25 19:00:00', 'RON', 'MANUAL', 'radu.ionescu@email.com', 'Supermarket', 'Lidl Iasi', 'Cumparaturi saptamanale'),
          (1026, 310.00, '2026-03-08 17:30:00', 'RON', 'MANUAL', 'maria.popa@email.com', 'Supermarket', 'Kaufland Pacurari', 'Cumparaturi de weekend la Kaufland'),
          (1027, 65.00, '2026-03-15 08:45:00', 'RON', 'MANUAL', 'mihai.popescu@email.com', 'Taxi', 'Uber / Bolt', 'Uber spre birou dimineata'),
          (1030, 35.00, '2026-03-22 09:15:00', 'RON', 'MANUAL', 'maria.popa@email.com', 'Cafenea', 'Starbucks Palas', 'Cafea de dimineata inainte de sedinta'),
          (1031, 80.00, '2026-03-28 18:00:00', 'RON', 'MANUAL', 'radu.ionescu@email.com', 'Medicamente', 'Farmacie Catena', 'Medicamente pentru alergia de primavara'),
          (1028, 650.00, '2026-04-10 14:00:00', 'RON', 'MANUAL', 'ana.popescu@email.com', 'Supermarket', 'Lidl Iasi', 'Cumparaturi mari pentru masa de Paste'),
          (1029, 180.00, '2026-04-20 19:30:00', 'RON', 'MANUAL', 'cristian.popa@email.com', 'Restaurant', 'Restaurant Vivo', 'Cina in oras dupa munca'),
          (1032, 105.00, '2026-04-04 19:00:00', 'RON', 'MANUAL', 'andrei.ionescu@email.com', 'Cinema', 'Cinema City', 'Iesit la un film nou in weekend'),
          (1033, 100.00, '2026-04-25 10:30:00', 'RON', 'MANUAL', 'ana.popescu@email.com', 'Supermarket', 'Kaufland Pacurari', 'Completare provizii dupa sarbatori'),
          (1034, 45.00, '2026-04-28 01:15:00', 'RON', 'MANUAL', 'sofia.popescu@email.com', 'Taxi', 'Uber / Bolt', 'Intoarcere acasa de la petrecere')
     ) AS v(id, amount, expense_date, currency, source_type, email, category_name, store_name, description)
         JOIN users u ON u.email = v.email
         JOIN family_members fm ON fm.user_id = u.id
         JOIN categories c ON c.name = v.category_name
         JOIN locations l ON l.store = v.store_name;

INSERT INTO expense_items (item_name, amount, expense_id, category_id, description, quantity)
SELECT v.item_name, v.amount, v.expense_id, c.id, v.description, v.quantity
FROM (VALUES
          ('Iaurt Grecesc', 12.30, 2, 'Supermarket', 'Iaurt Grecesc', 1),
          ('Cafea Boabe 500g', 45.20, 2, 'Supermarket', 'Cafea Boabe 500g', 1),
          ('Cereale Integrale', 28.00, 2, 'Supermarket', 'Cereale Integrale', 1),
          ('Pâine', 10.50, 3, 'Supermarket', 'Pâine proaspătă', 2),
          ('Alimente', 140.00, 3, 'Supermarket', 'Diverse alimente', 1),
          ('Paine Feliata', 10.00, 1001, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Lapte 3.5%', 15.00, 1001, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Oua Caserola', 10.00, 1001, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Detergent Vase', 25.00, 1001, 'Curatenie', 'Produse intretinere locuinta', 1),
          ('Sirop Tuse', 125.00, 1001, 'Medicamente', 'Tratament si medicatie', 1),
          ('Cursa scoala', 18.00, 1002, 'Taxi', 'Cost deplasare', 1),
          ('Bacsis sofer', 7.00, 1002, 'Taxi', 'Cost deplasare', 1),
          ('Faina', 30.00, 1003, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Carne de pui', 120.00, 1003, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Saci menajeri', 40.00, 1003, 'Curatenie', 'Produse intretinere locuinta', 1),
          ('Bureti de vase', 60.00, 1003, 'Curatenie', 'Produse intretinere locuinta', 1),
          ('Bilet Adult', 40.00, 1004, 'Cinema', 'Mancare si divertisment', 1),
          ('Bilet Copil', 30.00, 1004, 'Cinema', 'Mancare si divertisment', 1),
          ('Popcorn Mare', 30.00, 1004, 'Restaurant', 'Mancare si divertisment', 1),
          ('Suc Cola', 20.00, 1004, 'Restaurant', 'Mancare si divertisment', 1),
          ('Coaste de porc', 80.00, 1005, 'Restaurant', 'Mancare si divertisment', 1),
          ('Cartofi prajiti', 20.00, 1005, 'Restaurant', 'Mancare si divertisment', 1),
          ('Salata Coleslaw', 20.00, 1005, 'Restaurant', 'Mancare si divertisment', 1),
          ('Bere Neagra', 30.00, 1005, 'Restaurant', 'Mancare si divertisment', 1),
          ('Carne Vita', 120.00, 1006, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Rosii', 30.00, 1006, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Castraveti', 20.00, 1006, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Cartofi', 30.00, 1006, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Scutece', 120.00, 1006, 'Curatenie', 'Produse intretinere locuinta', 1),
          ('Frappuccino', 25.00, 1007, 'Cafenea', 'Mancare si divertisment', 1),
          ('Briosa Ciocolata', 20.00, 1007, 'Cafenea', 'Mancare si divertisment', 1),
          ('Peste proaspat', 80.00, 1008, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Orez', 20.00, 1008, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Legume congelate', 40.00, 1008, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Inghetata', 40.00, 1008, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Cursa intoarcere', 18.00, 1009, 'Taxi', 'Cost deplasare', 1),
          ('Bacsis sofer', 4.00, 1009, 'Taxi', 'Cost deplasare', 1),
          ('Paracetamol', 35.00, 1010, 'Medicamente', 'Tratament si medicatie', 1),
          ('Vitamina C', 30.00, 1010, 'Medicamente', 'Tratament si medicatie', 1),
          ('Plasturi', 20.00, 1010, 'Medicamente', 'Tratament si medicatie', 1),
          ('Analize de sange', 150.00, 1011, 'Consultatii', 'Tratament si medicatie', 1),
          ('Magneziu', 35.00, 1011, 'Medicamente', 'Tratament si medicatie', 1),
          ('Calciu', 25.00, 1011, 'Medicamente', 'Tratament si medicatie', 1),
          ('Burger Special', 45.00, 1012, 'Restaurant', 'Mancare si divertisment', 1),
          ('Cartofi Wedges', 20.00, 1012, 'Restaurant', 'Mancare si divertisment', 1),
          ('Bere IPA', 35.00, 1012, 'Restaurant', 'Mancare si divertisment', 1),
          ('Bere Blonda', 35.00, 1012, 'Restaurant', 'Mancare si divertisment', 1),
          ('Paste Carbonara', 60.00, 1013, 'Restaurant', 'Mancare si divertisment', 1),
          ('Pizza Diavola', 50.00, 1013, 'Restaurant', 'Mancare si divertisment', 1),
          ('Tiramisu', 30.00, 1013, 'Restaurant', 'Mancare si divertisment', 1),
          ('Limonada', 30.00, 1013, 'Restaurant', 'Mancare si divertisment', 1),
          ('Apa Plata', 30.00, 1013, 'Restaurant', 'Mancare si divertisment', 1),
          ('Bilet Film', 25.00, 1014, 'Cinema', 'Mancare si divertisment', 1),
          ('Nachos', 20.00, 1014, 'Restaurant', 'Mancare si divertisment', 1),
          ('Caffe Latte', 20.00, 1015, 'Cafenea', 'Mancare si divertisment', 1),
          ('Cappuccino', 20.00, 1015, 'Cafenea', 'Mancare si divertisment', 1),
          ('Croissant cu unt', 10.00, 1015, 'Cafenea', 'Mancare si divertisment', 1),
          ('Croissant ciocolata', 10.00, 1015, 'Cafenea', 'Mancare si divertisment', 1),
          ('Cursa serviciu', 25.00, 1016, 'Taxi', 'Cost deplasare', 1),
          ('Taxa vreme rea', 5.00, 1016, 'Taxi', 'Cost deplasare', 1),
          ('Bilet Film 3D', 40.00, 1017, 'Cinema', 'Mancare si divertisment', 1),
          ('Popcorn Caramel', 15.00, 1017, 'Restaurant', 'Mancare si divertisment', 1),
          ('Suc de mere', 15.00, 1017, 'Restaurant', 'Mancare si divertisment', 1),
          ('Picaturi Ochi', 35.00, 1018, 'Medicamente', 'Tratament si medicatie', 1),
          ('Servetele Umede', 20.00, 1018, 'Curatenie', 'Produse intretinere locuinta', 1),
          ('Cursa seara', 30.00, 1019, 'Taxi', 'Cost deplasare', 1),
          ('Bacsis sofer', 8.00, 1019, 'Taxi', 'Cost deplasare', 1),
          ('Supa Crema', 25.00, 1020, 'Restaurant', 'Mancare si divertisment', 1),
          ('Pui cu Gorgonzola', 45.00, 1020, 'Restaurant', 'Mancare si divertisment', 1),
          ('Espresso', 20.00, 1020, 'Cafenea', 'Mancare si divertisment', 1),
          ('Detergent Lichid', 100.00, 1021, 'Curatenie', 'Produse intretinere locuinta', 1),
          ('Ghiozdan Scoala', 150.00, 1021, 'Rechizite', 'Achizitie standard', 1),
          ('Caiete', 50.00, 1021, 'Rechizite', 'Achizitie standard', 1),
          ('Carne Pui', 100.00, 1021, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Mere', 50.00, 1021, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Cursa meditatii', 35.00, 1022, 'Taxi', 'Cost deplasare', 1),
          ('Bacsis', 5.00, 1022, 'Taxi', 'Cost deplasare', 1),
          ('Iced Latte', 25.00, 1023, 'Cafenea', 'Mancare si divertisment', 1),
          ('Cookie cu ciocolata', 10.00, 1023, 'Cafenea', 'Mancare si divertisment', 1),
          ('Antibiotic', 90.00, 1024, 'Medicamente', 'Tratament si medicatie', 1),
          ('Probiotic', 40.00, 1024, 'Medicamente', 'Tratament si medicatie', 1),
          ('Ibuprofen', 20.00, 1024, 'Medicamente', 'Tratament si medicatie', 1),
          ('Cascaval', 40.00, 1025, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Muschi File', 30.00, 1025, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Apa Minerala', 20.00, 1025, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Suc de portocale', 20.00, 1025, 'Supermarket', 'Alimente si provizii de baza', 1),
          ('Fructe bio', 60.00, 1026, 'Supermarket', 'Fructe pentru toata saptamana', 1),
          ('Produse curatenie', 150.00, 1026, 'Curatenie', 'Detergent si solutii de sters praful', 1),
          ('Cosmetice', 100.00, 1026, 'Supermarket', 'Sampon, gel de dus si pasta de dinti', 1),
          ('Cursa', 50.00, 1027, 'Taxi', 'Trafic infernal din cauza ploii', 1),
          ('Bacsis', 15.00, 1027, 'Taxi', 'Lasat bacsis in aplicatie', 1),
          ('Latte mare', 25.00, 1030, 'Cafenea', 'Cafea cu lapte de ovaz', 1),
          ('Croissant', 10.00, 1030, 'Cafenea', 'Mic dejun rapid la pachet', 1),
          ('Antihistaminice', 45.00, 1031, 'Medicamente', 'Pastile pentru polen', 1),
          ('Picaturi de ochi', 35.00, 1031, 'Medicamente', 'Calmarea iritatiilor', 1),
          ('Carne de miel', 250.00, 1028, 'Supermarket', 'Carne proaspata pentru friptura', 1),
          ('Oua si vopsea', 50.00, 1028, 'Supermarket', 'Oua albe si vopsea rosie', 1),
          ('Cozonac ambalat', 150.00, 1028, 'Supermarket', 'Doi cozonaci cu nuca si cacao', 1),
          ('Bauturi si vin', 200.00, 1028, 'Supermarket', 'Vin rosu si sucuri pentru musafiri', 1),
          ('Burger vita dublu', 90.00, 1029, 'Restaurant', 'Pofta de burger cu cartofi prajiti', 1),
          ('Bauturi artizanale', 60.00, 1029, 'Restaurant', 'Doua beri reci la draft', 1),
          ('Desert', 30.00, 1029, 'Restaurant', 'Un cheesecake impartit la doi', 1),
          ('Bilete premiera', 60.00, 1032, 'Cinema', 'Doua bilete in randul din mijloc', 1),
          ('Meniu popcorn si suc', 45.00, 1032, 'Restaurant', 'Popcorn mare si cola pentru film', 1),
          ('Apa plata bax', 20.00, 1033, 'Supermarket', 'Doua baxuri de apa la 2 litri', 1),
          ('Legume proaspete', 35.00, 1033, 'Supermarket', 'Rosii, castraveti si salata pentru dieta', 1),
          ('Paine si mezeluri', 45.00, 1033, 'Supermarket', 'Mic dejun pentru restul saptamanii', 1),
          ('Cursa tarif de noapte', 45.00, 1034, 'Taxi', 'Tarif dinamic aplicat dupa miezul noptii', 1)
     ) AS v(item_name, amount, expense_id, category_name, description, quantity)
         JOIN categories c ON c.name = v.category_name;

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM users;
SELECT setval(pg_get_serial_sequence('families', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM families;
SELECT setval(pg_get_serial_sequence('family_members', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM family_members;
SELECT setval(pg_get_serial_sequence('categories', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM categories;
SELECT setval(pg_get_serial_sequence('locations', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM locations;
SELECT setval(pg_get_serial_sequence('budgets', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM budgets;
SELECT setval(pg_get_serial_sequence('expenses', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM expenses;
SELECT setval(pg_get_serial_sequence('expense_items', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM expense_items;

COMMIT;

SELECT 'budgets_null_family_or_category' AS check_name, COUNT(*) AS rows_with_nulls
FROM budgets
WHERE family_id IS NULL OR category_id IS NULL;

SELECT 'expenses_null_user_family_category_location' AS check_name, COUNT(*) AS rows_with_nulls
FROM expenses
WHERE user_id IS NULL OR family_id IS NULL OR category_id IS NULL OR location_id IS NULL;

SELECT 'expense_items_null_expense_or_category' AS check_name, COUNT(*) AS rows_with_nulls
FROM expense_items
WHERE expense_id IS NULL OR category_id IS NULL;