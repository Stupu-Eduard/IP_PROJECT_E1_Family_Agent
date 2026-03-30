# Săptămâna 6: Planificare Modul Dumitrița & Arhitectură (teodor)

## 1. Arhitectură și Integrare (Sistem Monolitic)
- **Fără Microservicii:** Abordarea de a apela `ExtractionService` ca API extern (HTTP) a fost eliminată complet. Arhitectura proiectului este una **monolitică** clară (Spring Boot).
- **Abordarea Laurei:** Injectarea `ExtractionService` ca Spring Bean (folosind `@Autowired` / injectare prin constructor) în Pipeline este *modul corect* și optim de a face orchestrarea, eliminând complexitatea și latența rețelei.

## 2. Modulul Dumitriței: LLM Backend & Vector DB Communication
S-a observat la inspectarea profundă ("deep dive") a codului că Dumitrița a pus clase care cer `dev.langchain4j` (ex: `ChatLanguageModel`, `AiServices`), însă **aceste dependențe lipsesc complet din `pom.xml`**, așadar codul curent nu compilează și nu are implementarea mecanică cerută. Planul este complet revizuit astfel:

### A. Repararea Mecanismului de Request LLM
- Se vor adăuga în `pom.xml` dependențele aferente Langchain4J (`langchain4j`, `langchain4j-openrouter` sau integrări pentru suport REST direct).
- **Configurarea Cheilor:** Proiectul dispune deja de fișierul `.env` care conține `OPENROUTER_API_KEY` (pentru modelul Nemotron3) și `DEEPSEEK_API_KEY`. Se va crea o clasă `@Configuration` (ex: `LlmConfig.java`) unde aceste chei vor fin injectate cu `@Value("${OPENROUTER_API_KEY}")` pentru a instanția modelul LLM extern care va efectua prompturile efective, fără placeholder.

### B. Extracția Datelor & Comunicarea cu Qdrant
- Funcția de extracție din `ExtractionService` va genera prompturile către LLM (reprezentând NER-ul inteligent).
- Pentru **Embeddings**, LLM-ul va genera vectorii reali bazați pe OCR. Componenta Dumitriței va face apeluri prin REST folosind un `RestTemplate` (sau refolosind repository-ul implementat anterior) pentru a citi și scrie în instanța de Qdrant.

## 3. Testare (Unit Tests / Integration)
Planul inițial rigid (5 teste pentru regex) se va anula. Ne vom focusa pe validarea end-to-end a noii infrastructuri AI:
- **Test 1 - LLM External Call:** Un test care verifică dacă mecanismul de cereri către DeepSeek sau Nemotron3 obține un răspuns valid (JSON).
- **Test 2 - Qdrant Vector Validation:** Verificarea conexiunii către containerul de Qdrant (verificarea portului 6333 și căutarea dacă vectorii ajung / pot fi trași din DB).
- **Test 3 - PostgreSQL Validation:** Validarea prin Repository că tranzacția relațională a fost procesată corect alături de ID-urile vectorilor.
