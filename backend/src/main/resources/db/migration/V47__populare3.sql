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