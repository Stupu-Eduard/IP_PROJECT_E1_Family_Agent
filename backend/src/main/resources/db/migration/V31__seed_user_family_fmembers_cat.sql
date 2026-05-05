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