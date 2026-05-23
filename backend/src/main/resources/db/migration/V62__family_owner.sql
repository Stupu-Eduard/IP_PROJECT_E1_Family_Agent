ALTER TABLE families ADD COLUMN IF NOT EXISTS owner_id BIGINT REFERENCES users(id);

UPDATE families f
SET owner_id = (
    SELECT fm.user_id
    FROM family_members fm
    WHERE fm.family_id = f.id AND fm.role = 'Parent'
    ORDER BY fm.id ASC
    LIMIT 1
)
WHERE f.owner_id IS NULL;
