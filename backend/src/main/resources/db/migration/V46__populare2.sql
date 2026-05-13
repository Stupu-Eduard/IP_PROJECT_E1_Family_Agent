

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