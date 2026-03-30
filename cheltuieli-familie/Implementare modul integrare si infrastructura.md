                                                                                                            Implementare modul integrare si infrastructura

1.Configurarea mediului si a conexiunii: 
Am setat parametrii de conectare in Spring Boot pentru a comunica direct cu serverul extern. Am configurat adresa IP, portul, baza de date si credentialele necesare pentru ca aplicatia sa poata accesa datele in timp real.

2.Integrarea Flyway: 
Am implementat si configurat Flyway pentru a gestiona migrarile bazei de date. Acest lucru ne permite sa pastram aceeasi structura a tabelelor pe toate computerele echipei, schimbarile fiind aplicate automat la pornirea aplicatiei.

3.Definirea schemei initiale: 
Am scris scriptul SQL pentru crearea tabelelor principale, respectand relatiile dintre utilizatori, familii, categorii si locatii. Am adaugat constrangeri de integritate, cum ar fi cheile straine si validarea sumelor pozitive pentru cheltuieli.

4.Popularea cu date de test:
Am creat un script separat de tip seed data prin care am introdus in sistem primele categorii, o familie si un utilizator. Astfel, putem testa functionalitatile aplicatiei imediat, fara a fi nevoie de introducere manuala de date.

5.Integrarea stratului de date in Java:
Am creat entitatile JPA si interfetele de tip Repository pentru fiecare tabela definita, asigurand astfel comunicarea dintre codul Java si baza de date.

Verificare conformitate server
Configuratia pe care am implementat-o este pe deplin aplicabila si functionala pe baza specificatiilor serverului:
    Host: 207.154.220.168 (configurat corect)
    Database: main_db (mapat in URL-ul de conexiune)
    User: FamilyDB (utilizat pentru autentificare)
