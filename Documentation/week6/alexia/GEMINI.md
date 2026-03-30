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

### Phase 1: Infrastructure & Initial Mockups
- **Prompt:** "Generate ExpenseEntity.java using Jakarta Persistence with id, amount, category, date, and rawText."
- **Prompt:** "Create ExpenseVectorRepositoryImpl.java with a saveVector method using a float[10] dummy placeholder."
- **Prompt:** "Generate docker-compose.yml for postgres:16-alpine and qdrant services with persistent volumes."
- **Prompt:** "Implement VectorController.java with GET /api/v1/vectors/check/{id} to validate Qdrant synchronization."

### Phase 2: Production Integration & Documentation
- **Prompt:** "Update pom.xml to include the official io.qdrant:client dependency version 1.10.1."
- **Prompt:** "Rewrite ExpenseVectorRepositoryImpl.java to use a real RestTemplate client for Qdrant REST API (Port 6333), replacing all 'return true' dummy logic with actual HTTP status checks."
- **Prompt:** "Update application.yml with Qdrant host, port 6333, and collection-name: expenses."
- **Prompt:** "Create a professional README.md including Hybrid Architecture, Gemini CLI Analysis (Pros/Cons), and the theory that Candidate Keys are calculated using Functional Dependencies (Σ) only, ignoring Multi-valued Dependencies (Δ)."

## 4. DATABASE THEORY
Candidate Keys are calculated using Functional Dependencies (Σ) only. Multi-valued Dependencies (Δ) are explicitly **NOT** used for key calculation, focusing on direct relationships and normalization.

## 5. TECH STACK
- **Backend:** Java 21, Spring Boot 3, Jakarta Persistence.
- **Databases:** PostgreSQL 16 (Relational), Qdrant (Vector DB).
