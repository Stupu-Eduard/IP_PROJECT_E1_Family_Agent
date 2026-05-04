
ALTER TABLE categorii RENAME TO categories;
ALTER TABLE cheltuieli RENAME TO expenses;



ALTER TABLE categories RENAME COLUMN nume TO name;

ALTER TABLE families RENAME COLUMN nume TO name;

ALTER TABLE users RENAME COLUMN nume TO name;
ALTER TABLE users RENAME COLUMN parola TO password;


ALTER TABLE family_members RENAME COLUMN rol TO role;


ALTER TABLE locations RENAME COLUMN nume TO name;
ALTER TABLE locations RENAME COLUMN adresa TO adress;


ALTER TABLE expenses RENAME COLUMN suma TO sum;
ALTER TABLE expenses RENAME COLUMN descriere TO description;
ALTER TABLE expenses RENAME COLUMN data_cheltuiala TO date;
ALTER TABLE expenses RENAME COLUMN categorie_id TO category_id;