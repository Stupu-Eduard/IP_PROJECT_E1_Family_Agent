-- Add admin account
INSERT INTO users (name, email, password_h, created_at)
SELECT 'Admin', 'admin@admin.com', 'admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@admin.com');
