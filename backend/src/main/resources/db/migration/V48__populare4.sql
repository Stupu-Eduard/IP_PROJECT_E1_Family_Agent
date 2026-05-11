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