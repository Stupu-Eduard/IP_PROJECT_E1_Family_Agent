INSERT INTO categorii (nume) VALUES ('Mâncare'), ('Chirie'), ('Transport'), ('Divertisment');
INSERT INTO families (nume) VALUES ('Familia Ionescu');
INSERT INTO users (nume, email, parola, family_id) VALUES
('Ion Ionescu', 'ion@email.com', 'parola_securizata_123', 1);
INSERT INTO family_members (user_id, family_id, rol) VALUES (1, 1, 'ADMIN');
INSERT INTO locations (nume, adresa) VALUES
('Kaufland', 'Str. Principală nr. 1'),
('Benzinăria OMV', 'Bd. Independenței');
INSERT INTO cheltuieli (suma, descriere, categorie_id, user_id, location_id) VALUES
(150.00, 'Cumpărături supermarket', 1, 1, 1);