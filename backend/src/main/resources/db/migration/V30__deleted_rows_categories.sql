--move the objects if they exist
UPDATE expenses
SET category_id = (
    SELECT MIN(c2.id)
    FROM categories c2
    WHERE c2.name = (SELECT c3.name FROM categories c3 WHERE c3.id = expenses.category_id)
)
WHERE category_id IN (
    SELECT id FROM categories WHERE name IN (
        SELECT name FROM categories GROUP BY name HAVING COUNT(*) > 1
    )
);

--delete duplicate categories
DELETE FROM categories
WHERE id NOT IN (
    SELECT min_id FROM (
                           SELECT MIN(id) as min_id FROM categories GROUP BY name
                       ) as temp
);

--made unique
ALTER TABLE categories ADD CONSTRAINT uk_category_name UNIQUE (name);