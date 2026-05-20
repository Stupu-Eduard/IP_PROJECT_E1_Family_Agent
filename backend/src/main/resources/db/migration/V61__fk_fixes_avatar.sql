-- expenses: user_id devine NULL la stergerea userului (datele raman)
ALTER TABLE expenses DROP CONSTRAINT IF EXISTS expenses_user_id_fkey;
ALTER TABLE expenses
    ADD CONSTRAINT expenses_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- answers: se sterg automat odata cu userul
ALTER TABLE answers DROP CONSTRAINT IF EXISTS answers_user_id_fkey;
ALTER TABLE answers
    ADD CONSTRAINT answers_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- budgets: user_id devine NULL la stergerea userului
ALTER TABLE budgets DROP CONSTRAINT IF EXISTS budgets_user_id_fkey;
ALTER TABLE budgets
    ADD CONSTRAINT budgets_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- user_sessions: se sterg automat odata cu userul
ALTER TABLE user_sessions DROP CONSTRAINT IF EXISTS fk_user_session;
ALTER TABLE user_sessions
    ADD CONSTRAINT fk_user_session
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- coloana noua pentru avatar (safe, nu afecteaza nimic existent)
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);