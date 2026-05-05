-- 1. Tabel pentru Sesiunile Userilor
CREATE TABLE IF NOT EXISTS user_sessions (
                                             id SERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             session_token VARCHAR(255),
    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_session FOREIGN KEY (user_id) REFERENCES users(id)
    );

-- 2. Functia care trimite notificarea in timp real (folosind PG_NOTIFY)
CREATE OR REPLACE FUNCTION notify_location_update()
RETURNS TRIGGER AS $$
BEGIN
    -- Trimite un mesaj pe canalul 'location_updates' cu ID-ul locatiei modificate
    PERFORM pg_notify('location_updates', row_to_json(NEW)::text);
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Trigger-ul care se activeaza la orice INSERT sau UPDATE in tabelul locations
DROP TRIGGER IF EXISTS trg_location_update ON locations;
CREATE TRIGGER trg_location_update
    AFTER INSERT OR UPDATE ON locations
                        FOR EACH ROW
                        EXECUTE FUNCTION notify_location_update();