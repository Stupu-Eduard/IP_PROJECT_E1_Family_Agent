# IP_PROJECT_E1_Family_Agent# Week 6 – Teodor: LLM Integration & End-to-End Pipeline

## 1. Obiectiv

Integrarea unui LLM extern (DeepSeek) în aplicația Spring Boot monolitică pentru extracția structurată de date financiare din text OCR românesc, persistarea în PostgreSQL și stocarea vectorilor dummy în Qdrant.

---

## 2. Ce s-a realizat

### 2.1 Clasa `FamilyAgentApplication`
- Creată clasa principală Spring Boot cu `@SpringBootApplication`.
- Fără aceasta aplicația nu pornea (`Unable to find a suitable main class`).
- Locație: `src/main/java/com/proiect/FamilyAgentApplication.java`

---

### 2.2 Integrare LangChain4j (DeepSeek API)

**Fișiere modificate:**
- `pom.xml` – adăugate dependențele:
  ```xml
  <dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j</artifactId>
      <version>0.31.0</version>
  </dependency>
  <dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j-open-ai</artifactId>
      <version>0.31.0</version>
  </dependency>
  ```

- `src/main/java/com/proiect/config/LlmConfig.java` – Bean Spring care:
  - Citește `DEEPSEEK_API_KEY` sau `OPENROUTER_API_KEY` din variabile de mediu (`.env`)
  - Configurează `OpenAiChatModel` cu `temperature(0.1)` și `baseUrl` corect
  - **Prioritate:** DeepSeek dacă cheia există, altfel OpenRouter

**Notă OpenRouter:** Modelul `nvidia/nemotron-4-340b-instruct` este offline. Se poate înlocui cu alt model din catalog în `LlmConfig.java`.

---

### 2.3 Prompt Engineering Determinist

**Fișier:** `src/main/java/com/proiect/service/ExtractionService.java`

Interfața `ExtractionAssistant` trimite un prompt strict JSON către LLM:

```json
{
  "role": "Financial Receipt Expert Analyst",
  "goal": "Extract deterministic financial data from raw OCR Romanian text",
  "inputs": {"raw_text": "{{rawText}}"},
  "style": { "audience": "system_DB", "tone": "stoic_deterministic" },
  "rules": ["No conversational text", "Infer location if obvious", "Default to RON"],
  "steps": ["Scan for monetary amounts", "Identify category", "Extract dates"],
  "output": {
    "format": "json",
    "schema_note": "Strict JSON. Types matter.",
    "example_shape": {
      "amount": "decimal",
      "category": "string",
      "location": "string",
      "person": "string",
      "transactionDate": "YYYY-MM-DD"
    }
  },
  "params": {"temperature": 0.1, "max_tokens": 500}
}
```

**Fix aplicat:** DeepSeek returnează JSON-ul învelit în ` ```json ... ``` `. S-a adăugat metoda `stripMarkdownFences()` care curăță automat envelope-ul înainte de parsare cu `ObjectMapper`.

---

### 2.4 Integrare Qdrant (REST via RestTemplate)

**Fișier:** `src/main/java/com/proiect/repository/ExpenseVectorRepositoryImpl.java`

- **Eliminat** dependency-ul `io.qdrant:client:1.10.1` din `pom.xml` (nu exista în Maven Central).
- Implementare prin `RestTemplate` direct pe REST API-ul Qdrant:
  - `PUT /collections/expenses/points?wait=true` – upsert vector
  - `GET /collections/expenses/points/{id}` – verificare existență
- Colecția `expenses` a fost creată manual cu vectori de dimensiune 10, distanță Cosine:
  ```bash
  curl -X PUT http://localhost:6333/collections/expenses \
    -H "Content-Type: application/json" \
    -d '{"vectors": {"size": 10, "distance": "Cosine"}}'
  ```
- Adăugate timeout-uri (connect: 3s, read: 5s) pe `SimpleClientHttpRequestFactory`.

---

### 2.5 Fixuri Arhitecturale

| Fișier | Problemă | Fix |
|---|---|---|
| `ExpenseVectorRepositoryImpl.java` | Conținea atât interfața cât și implementarea în același fișier (eroare Java) | Separat corect; implementarea mută dependența `ExpenseEntity` |
| `ExpensePipelineService.java` | Folosea constructor cu 5 argumente inexistent pe `ExpenseEntity` | Înlocuit cu `new ExpenseEntity()` + setters |
| `SyncService.java` | Transmitea `Long` în loc de `ExpenseEntity` la `saveVector()` | Corectat tipul parametrului |
| `application.yml` | Credențiale DB `postgres/postgres` care nu corespundeau docker-compose (`user/password`) | Sincronizate |
| `mvnw` | Path separator incorect (`\`), lipsea `-Dmaven.multiModuleProjectDirectory` | Reparat |
| `.gitignore` | Lipsea `target/` – Git tracka fișierele `.class` compilate | Adăugat `target/` și fișierele Maven standard |

---

### 2.6 Validare Pipeline End-to-End

**Infrastructură rulată local via Docker Compose:**
- PostgreSQL 16 pe portul `5432`
- Qdrant pe portul `6333`

**Test real executat:**

```bash
curl -X POST "http://localhost:8080/api/v1/pipeline/process" \
  -H "Content-Type: application/json" \
  -d '{"rawText": "Astazi 24 Aprilie, am mancat la KFC din mall 45.5 lei pe un meniu."}'
```

**Rezultat confirmat în log-uri:**

```
✅ DeepSeek LLM  → amount=45.5, category=food, location=KFC din mall
✅ PostgreSQL    → Saved entity to SQL Database: ID=4
✅ Qdrant        → Entity ID 4 confirmed in Qdrant vector store.
✅ Validation    → Validation successful for ID: 4
✅ Pipeline      → Pipeline completed successfully for ID: 4
```

---

## 3. Stare curentă

| Componentă | Status |
|---|---|
| DeepSeek API (LLM extern) | ✅ Funcțional |
| Extracție JSON din text românesc | ✅ Funcțională |
| Persistare PostgreSQL | ✅ Funcțională |
| Stocare vector Qdrant | ✅ Funcțională (vector dummy) |
| Embedding real (semantic) | ⚠️ Dummy – vector aleatoriu `float[10]` |
| OpenRouter / Nemotron3 | ❌ Endpoint offline la provider |

---

## 4. Ce rămâne de făcut

1. **Embedding real** – înlocuirea vectorului dummy cu un model de embedding (ex. `sentence-transformers`, `text-embedding-ada-002`, sau un model local).
2. **OpenRouter fallback** – alegerea unui model disponibil (ex. `meta-llama/llama-3.1-8b-instruct`) în `LlmConfig.java`.
3. **Teste de integrare** – teste JUnit cu `@SpringBootTest` și Testcontainers pentru PostgreSQL + Qdrant.

---

## 5. Cum se pornește

```bash
# 1. Asigură-te că Docker Desktop rulează cu WSL 2 integration activată

# 2. Pornește infrastructura
docker-compose up -d

# 3. Pornește aplicația (cu cheile API din .env)
source .env && export DEEPSEEK_API_KEY OPENROUTER_API_KEY
./mvnw spring-boot:run

# 4. Testează pipeline-ul
curl -X POST "http://localhost:8080/api/v1/pipeline/process" \
  -H "Content-Type: application/json" \
  -d '{"rawText": "Am platit 89 lei la Mega Image ieri."}'
```
