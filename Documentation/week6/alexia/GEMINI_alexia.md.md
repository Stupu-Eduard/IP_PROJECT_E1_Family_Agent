# MODULE M3: PERSISTENCE & VECTOR STORE (ALEXIA)

## 1. RESPONSIBILITY
Configuring the Vector Database (Qdrant) infrastructure and implementing the hybrid persistence layer for financial entities using dummy embeddings (size 10) as placeholders.

## 2. ARCHITECTURE & WORKFLOW
**Input:** Structured DTO from Dumitrita's Extraction API.

**Process:**
1. Save relational data to PostgreSQL 16.
2. Generate a 10-dimension dummy vector.
3. Save semantic data to Qdrant.

**Output:** Confirmed Vector ID for validation.

## 3. AI INTERACTION LOG (PROMPTS)
- Generate `ExpenseEntity.java` using Jakarta Persistence with id, amount, category, date, and rawText.
- Create `ExpenseVectorRepositoryImpl.java` with a `saveVector` method using a `float[10]` dummy placeholder.
- Generate `docker-compose.yml` for `postgres:16-alpine` and `qdrant` services with persistent volumes.
- Implement `VectorController.java` with `GET /api/v1/vectors/check/{id}` to validate Qdrant synchronization.

## 4. TECH STACK
- **Backend:** Java 21, Spring Boot 3, Jakarta Persistence.
- **Databases:** PostgreSQL 16 (Relational), Qdrant (Vector DB).
