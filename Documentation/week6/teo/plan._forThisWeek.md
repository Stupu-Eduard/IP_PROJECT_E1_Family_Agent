1. Dumitrița – Modulul Extraction API (NER Primitiv)
Responsabilitate Formală:
Dezvoltarea unui micro-modul de Natural Language Processing (NLP) procedural prin metode deterministe (regex-based parsing), expus ca RESTful API intern. Scop: transformarea inputului nestructurat (limbaj natural românesc) în structuri de date tipizate (DTO), fără dependință de LLM extern în această fază.
Plan Detaliat de Implementare (2-3 ore):
Definirea Contractului API (OpenAPI Spec)
Endpoint: POST /api/v1/extract
Request Body: RawInputDTO { String rawText; }
Response: ExtractedExpenseDTO { BigDecimal amount; String currency; String category; LocalDate transactionDate; String rawText; }
Coduri HTTP: 200 OK, 422 Unprocessable Entity (pentru date non-parsabile).
Implementarea Stratului de Serviciu (Service Layer)
Clasa ExtractionService cu metodă extract(RawInputDTO input).
Algoritm determinist bazat pe java.util.regex.Pattern:
Pattern pentru sume: (\\d+(?:[.,]\\d{1,2})?)\\s*(lei|RON|€|\\$)?
Pattern pentru categorii (whitelist): (cafea|taxi|mâncare|transport|supermarket) (case-insensitive).
Pattern pentru date relative: (azi|ieri|alaltăieri) → mapare la LocalDate.now().minusDays(...).
Normalizare: conversie virgulă în punct pentru separator decimal.
Controller & Global Exception Handler
@RestController cu @RequestMapping("/api/v1").
@ExceptionHandler(MethodArgumentNotValidException.class) pentru validare input.
Logging structurat (SLF4J) pentru fiecare cerere procesată.
Testare Unitara Immediată
Clasa ExtractionServiceTest (JUnit 5): minim 5 cazuri de test (input valid, sumă cu virgulă, monedă implicită, categorie necunoscută → default "diverse", dată relativă).
Deliverable: Modulul compilabil, testabil via Postman cu input-uri precum "50 lei taxi ieri" sau "12.5 € cafea azi".
2. Alexia – Modulul Vector Store & Persistence
Responsabilitate Formală:
Configurarea infrastructurii de Vector Database (Qdrant) și implementarea stratului de persistență pentru reprezentări semantice (placeholder) ale entităților financiare, cu expunerea capacităților CRUD și de căutare prin API.
Plan Detaliat de Implementare (2-3 ore):
Provisioning Infrastructură (Infrastructure as Code)
Fișier docker-compose.yml în root proiect: servicii postgres (imagine oficială 16-alpine) și qdrant (imagine qdrant/qdrant:latest).
Configurare porturi: PostgreSQL 5432, Qdrant 6333 (REST) / 6334 (gRPC).
Volume persistente: ./data/postgres și ./data/qdrant.
Configurare Aplicație Spring
application.yml:
Datasource PostgreSQL (JDBC URL, credentials).
Proprietăți Qdrant: qdrant.host=localhost, qdrant.port=6333, qdrant.collection-name=expenses.
Dependință Maven: qdrant-client (oficial Java client) sau REST Template custom.
Definirea Modelului de Date
Entitate JPA (PostgreSQL): ExpenseEntity { Long id; BigDecimal amount; String currency; String category; LocalDate date; String rawText; }.
Mapper către Vector Point: conversie în PointStruct (Qdrant API) cu vector dummy de dimensiune 10 (ex: [0.1, 0.1, ...]) – placeholder pentru embeddings reali.
Repository Pattern Dublu
ExpenseJpaRepository (Spring Data JPA) pentru date tranzacționale.
ExpenseVectorRepository (custom implementation): metode saveVector(ExpenseEntity, float[] vector) și searchSimilar(float[] queryVector, int limit) – în această fază returnează toate recordurile (mock search).
Endpoint de Verificare
GET /api/v1/vectors/status – returnează numărul de puncte din colecția Qdrant (health check).
Deliverable: Stack dockerizat pornit, endpoint-uri funcționale pentru salvare și "căutare" (mock), schema PostgreSQL generată automat (DDL) sau via Flyway script V1.
3. Laura – Modulul Integration & Data Pipeline
Responsabilitate Formală:
Dezvoltarea stratului de Integration Logic (Orchestration Layer) care implementează fluxul ETL (Extract-Transform-Load) end-to-end: consumul API-ului de extracție (Dumitrița), procesarea tranzacțională și persistența duală (SQL + Vector).
Plan Detaliat de Implementare (2-3 ore):
Client HTTP pentru Consum Intern
Definire RestTemplate bean configurat (timeout 5s).
Clasa ExtractionClient care apelează http://localhost:8080/api/v1/extract ( Dumitrița’s endpoint) și mapează răspunsul în ExtractedExpenseDTO.
Serviciul de Orchestrare (Transactional Boundary)
Clasa ExpensePipelineService cu metodă @Transactional public void processRawInput(String rawText).
Flux:
a. ExtractionClient.extract(rawText) → obține DTO.
b. Mapare DTO → ExpenseEntity (JPA).
c. Salvare în PostgreSQL (expenseJpaRepository.save).
d. Generare vector dummy (ex: new float[10]) și apel expenseVectorRepository.saveVector(entity, vector).
Controller de Integrare
POST /api/v1/pipeline/process – primește text brut, declanșează pipeline-ul, returnează ID-ul entității create și confirmare duplicare în Qdrant.
Verificare Consistenței (Constraint Validation)
Implementare PipelineValidationService care verifică post-condiții:
Existența în PostgreSQL după commit.
Existența în Qdrant (verificare punct prin ID).
Excepție custom PipelineException în caz de inconsistență.
Deliverable: Flux end-to-end funcțional: curl -X POST localhost:8080/api/v1/pipeline/process -d "100 lei supermarket ieri" creează record atât în PostgreSQL cât și în Qdrant, cu confirmare ID.