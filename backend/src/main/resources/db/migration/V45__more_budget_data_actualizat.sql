-- 1. FAMILIA POPESCU (Gama completa de bugete)
-- Utilitati
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1200.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Utilitati')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popescu') AND category_id = (SELECT id FROM categories WHERE name = 'Utilitati'));
-- Curatenie
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 350.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Curatenie')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popescu') AND category_id = (SELECT id FROM categories WHERE name = 'Curatenie'));

-- Vacanta de vara (Divertisment)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 5000.00, '2025-06-01', '2025-08-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Divertisment')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE amount = 5000.00 AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));

-- Cadouri Craciun
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1500.00, '2025-12-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Altele')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Altele') AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));

-- Hobby
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 400.00, '2025-01-01', '2025-12-31',
    (SELECT id FROM families WHERE name = 'Familia Popescu'),
    (SELECT id FROM categories WHERE name = 'Divertisment')
WHERE NOT EXISTS (
    SELECT 1 FROM budgets
    WHERE amount = 400.00
    AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu')
    AND category_id = (SELECT id FROM categories WHERE name = 'Divertisment')
);

-- 2. FAMILIA IONESCU (Focus Educatie si Casa)
-- Educatie
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 2000.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Educatie')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu') AND category_id = (SELECT id FROM categories WHERE name = 'Educatie'));

-- Mancare
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 2500.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Mâncare')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu') AND category_id = (SELECT id FROM categories WHERE name = 'Mâncare'));

-- Imbracaminte
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 800.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Shopping')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu') AND category_id = (SELECT id FROM categories WHERE name = 'Shopping'));

-- Casa (Reparatii/Intretinere)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1000.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Pentru casa')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Pentru casa') AND family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu'));

-- Abonamente (Netflix, Sala, etc)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 250.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Ionescu'), (SELECT id FROM categories WHERE name = 'Streaming')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE category_id = (SELECT id FROM categories WHERE name = 'Streaming') AND family_id = (SELECT id FROM families WHERE name = 'Familia Ionescu'));

-- 3. FAMILIA POPA (Focus Sanatate si Economii)
-- Sanatate
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 1200.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Sănătate')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popa') AND category_id = (SELECT id FROM categories WHERE name = 'Sănătate'));

-- Transport
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 600.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Transport')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popa') AND category_id = (SELECT id FROM categories WHERE name = 'Transport'));

-- Mancare
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 2100.00, '2025-01-01', '2025-12-31', (SELECT id FROM families WHERE name = 'Familia Popa'), (SELECT id FROM categories WHERE name = 'Mâncare')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE family_id = (SELECT id FROM families WHERE name = 'Familia Popa') AND category_id = (SELECT id FROM categories WHERE name = 'Mâncare'));

-- Economii (Fond de urgenta)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 3000.00, '2025-01-01', '2025-12-31',
    (SELECT id FROM families WHERE name = 'Familia Popa'),
    (SELECT id FROM categories WHERE name = 'Altele')
WHERE NOT EXISTS (
    SELECT 1 FROM budgets
    WHERE amount = 3000.00
    AND category_id = (SELECT id FROM categories WHERE name = 'Altele')
    AND family_id = (SELECT id FROM families WHERE name = 'Familia Popa')
);
-- Medicamente
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 500.00, '2025-01-01', '2025-12-31',
    (SELECT id FROM families WHERE name = 'Familia Popa'),
    (SELECT id FROM categories WHERE name = 'Medicamente')
WHERE NOT EXISTS (
    SELECT 1 FROM budgets
    WHERE amount = 500.00 -- Verificăm suma de 500
    AND category_id = (SELECT id FROM categories WHERE name = 'Medicamente')
    AND family_id = (SELECT id FROM families WHERE name = 'Familia Popa')
);

-- 4. BUGETE PE TERMEN SCURT (Saptamanale)
--  (Mancare pentru sarbatori)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 400.00, '2025-01-01', '2025-01-07', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Mâncare')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE amount = 400.00 AND start_date = '2025-01-01' AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));

-- (Mancare pentru sarbatori)
INSERT INTO budgets (amount, start_date, end_date, family_id, category_id)
SELECT 400.00, '2024-12-24', '2024-12-31', (SELECT id FROM families WHERE name = 'Familia Popescu'), (SELECT id FROM categories WHERE name = 'Mâncare')
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE amount = 400.00 AND start_date = '2024-12-24' AND family_id = (SELECT id FROM families WHERE name = 'Familia Popescu'));