# DOCUMENTAȚIE M3 - Modul Extracție și Normalizare AI
**Proiect:** Family Agent  
**Responsabil:** Dumitrița (Echipa M3)  
**Versiune:** 1.0  
**Data:** 19 Aprilie 2026

## 1. Rezumat Obiectiv
Modulul **M3 (Extraction & Normalization)** are rolul de a transforma datele nestructurate (text brut, fișiere PDF, bonuri fiscale) în entități financiare structurate și normalizate. Utilizând modele de limbaj (LLM via LangChain4j) și tehnici de procesare a textului, modulul extrage automat: suma, moneda, categoria, locația și persoana responsabilă pentru fiecare cheltuială.

## 2. Jurnal de Prompting
În procesul de dezvoltare au fost utilizate următoarele strategii de prompting pentru a ghida asistentul AI:
*   **Structura Spring Boot:** "Generează scheletul pentru un serviciu de extracție folosind Spring Boot 3 și LangChain4j, respectând pachetul com.proiect."
*   **Logica NER (Named Entity Recognition):** "Extrage din textul primit următoarele entități: amount, currency, category, location, person. Dacă suma nu e clară, păstrează textul original pentru normalizarea ulterioară."
*   **Contract JSON Strict:** "Sistem: Ești un asistent financiar. Returnează EXCLUSIV un obiect JSON valid, fără explicații suplimentare sau markdown tags."
*   **Integrare PDFBox/OCR:** "Implementează un serviciu care să preia un fișier multipart PDF și să returneze tot textul extras folosind Apache PDFBox."

## 3. Fixuri Arhitecturale
### Compatibilitate JDK 25 & Lombok
Din cauza incompatibilităților între versiunea de JDK 25 și adnotările Lombok în faza de compilare (eroarea `UNKNOWN TypeTag`), am procedat la:
*   **Eliminarea Lombok:** Am înlocuit `@Getter`, `@Setter`, `@Builder` și `@RequiredArgsConstructor` cu implementări manuale.
*   **Implementare Manuală Builder:** Am creat clase `Builder` statice interne pentru `ExpenseEntity` și `ExtractionResponse` pentru a menține fluența codului fără a depinde de procesorul de adnotări.
*   **Logging Standard:** Am înlocuit `@Slf4j` cu `java.util.logging.Logger`.

### Unificarea ExpenseEntity
Am extins entitatea originală pentru a acoperi toate cerințele de audit și analiză:
*   Adăugarea câmpului `date` (unificat cu `transactionDate`).
*   Includerea `rawInput` pentru a păstra sursa originală a datelor în caz de dispute sau re-extracție.
*   Utilizarea obligatorie a `BigDecimal` pentru `amount` pentru a evita erorile de precizie în calculele financiare.

## 4. Stare Curentă
*   **Sprint-uri 1-3:** Finalizate conform planului.
*   **Normalizare:** Suportă termeni românești precum "ieri", "alaltăieri", "o sută jumate", "ianuarie", etc.
*   **Endpoint-uri:** `/v1/upload/pdf` și `/v1/extract` sunt funcționale. Prefixul `/api` a fost eliminat conform regulilor echipei.
*   **Teste:** `PdfExtractionServiceTest` trece cu succes (`BUILD SUCCESS`).

## 5. Ce rămâne de făcut
*   **Tesseract OCR:** Optimizarea citirii bonurilor scanate (imagini) prin integrarea completă a librăriei `tess4j` (configurarea folderului `tessdata`).
*   **Integrare Analytics:** Dezvoltarea unui Feign Client sau a unui serviciu de notificare pentru a trimite cheltuielile normalizate către modulul de Analytics (Laura).
*   **Error Handling:** Rafinarea mesajelor de eroare pentru utilizatorii finali în interfața grafică.

## 6. Ghid de Pornire
Pentru a rula modulul local, urmați acești pași:

### A. Baza de Date
1. Deschideți PostgreSQL (pgAdmin sau psql).
2. Creați baza de date: `CREATE DATABASE family_agent_db;`
3. Asigurați-vă că userul `postgres` are parola setată la `admin` (configurație implicită în `application.yml`).

### B. Variabile de Mediu
Setați următoarele variabile de mediu înainte de pornire:
*   `DB_PASSWORD=admin`
*   `DEEPSEEK_API_KEY=cheia_ta_de_la_deepseek`

### C. Comenzi Rulare
```bash
# Executare teste unitare
mvn test

# Pornire aplicație
mvn spring-boot:run
```
