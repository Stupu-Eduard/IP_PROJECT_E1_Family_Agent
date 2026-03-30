# Family Agent - Module M3 (Persistence & Vector Store)

## 1. Overview
This module implements the core persistence layer for the Family Agent project, utilizing a hybrid storage architecture to support both traditional relational transactions and advanced semantic search capabilities.

## 2. Architecture & Workflow
The system employs a **Hybrid Storage Architecture** orchestrated by the `SyncService`:
- **PostgreSQL 16:** Handles relational data and ACID transactions for financial records.
- **Qdrant (Port 6333):** Acts as the Vector Database for storing embeddings, enabling semantic retrieval and RAG (Retrieval-Augmented Generation).
- **SyncService:** Ensures atomicity by synchronizing data between the relational database and the vector store during expense creation.

## 3. Technical Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.2.4
- **Persistence:** Jakarta Persistence (JPA), Hibernate
- **Infrastructure:** Docker & Docker Compose
- **Databases:** PostgreSQL 16 (Relational), Qdrant (Vector DB)
- **Communication:** RestTemplate for Qdrant REST API integration

## 4. Implementation Details
### ExpenseVectorRepositoryImpl
The vector persistence layer is implemented using Spring's `RestTemplate` to interact directly with the Qdrant REST API:
- **POST Operations:** Used to save points to the `expenses` collection. The implementation maps the relational ID to the Qdrant point ID and attaches a payload containing semantic metadata.
- **GET Operations:** Used for validation via the `/api/v1/vectors/check/{id}` endpoint. It performs a real-time status check (HTTP 200 OK) to confirm synchronization success.

## 5. Database Theory
In this project, **Candidate Keys** are calculated strictly using **Functional Dependencies (Σ)**. This ensures that the relational schema is optimized for data integrity and normalization. **Multi-valued Dependencies (Δ)** are explicitly **NOT** used for key calculation, focusing on clear, direct relationships within the financial entities.

## 6. Gemini CLI Analysis: Advantages & Disadvantages
During the development of Module M3, the following observations were made regarding the use of Gemini CLI:

### Pros
- **High Speed for Boilerplate:** Rapid generation of `pom.xml`, Jakarta Persistence entities, and repository structures.
- **Automated File Writing:** Seamless creation and modification of complex file structures across multiple directories.
- **Consistent Coding Standards:** Ensures that naming conventions and architectural patterns remain uniform across the codebase.

### Cons
- **Shell Syntax Errors:** Occasional conflicts between PowerShell and CMD syntax during automated command execution.
- **Context Window Limits:** Performance can degrade in extremely long sessions, requiring strategic context management.
- **Manual Cleanup:** Occasional need for manual intervention regarding markdown markers or file headers.

## 7. Setup & Execution
To get the environment and application running:

1. **Start Infrastructure:**
   ```bash
   docker-compose up -d
   ```
2. **Run Application:**
   ```bash
   mvn spring-boot:run
   ```
