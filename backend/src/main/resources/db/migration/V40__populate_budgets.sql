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

-- Buget Sanatate pentru Familia Popa (1000 lei pe luna)
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