ALTER TABLE users RENAME COLUMN password TO password_h;
ALTER TABLE users DROP COLUMN family_id;