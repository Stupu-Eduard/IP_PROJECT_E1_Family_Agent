## ATOMIC PLAN — Family Agent (50-60% Completion Target)
### Timeline: Weeks 7-9 (3 sprints to ~55%)
---
DUMITRIȚA — AI Extraction & Normalization
Sprint 1 (Week 7) — PDF Ingestion & Unified Entity
Step 1.1 — Add Apache PDFBox to pom.xml:
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.4</version>
</dependency>
Step 1.2 — Create PdfExtractionService in com.proiect.service:
@Service @Slf4j @RequiredArgsConstructor
public class PdfExtractionService {
    public String extractText(MultipartFile pdfFile) { ... }  // PDFBox PDDocument → text
    public List<String> extractPages(MultipartFile pdfFile) { ... }  // page-by-page
}
- Use Loader.loadPDF(file.getBytes()) to parse
- Extract text per page → List<String> for chunking
- Handle encrypted/empty PDFs → throw PipelineException
Step 1.3 — Create FileUploadController in com.proiect.controller:
@RestController @RequestMapping("/api/v1/upload") @Slf4j @RequiredArgsConstructor
public class FileUploadController {
    @PostMapping("/pdf")   // MultipartFile → PdfExtractionService → ExtractionService
    @PostMapping("/image") // MultipartFile → future OCR
}
- Accept MultipartFile, validate it's a PDF (check contentType == "application/pdf")
- Pass to PdfExtractionService.extractText() → then to ExtractionService.process()
Step 1.4 — Unify the dual entity model. Delete ExpenseEntity (the minimal one). Rename ExpenseEntityDumitrita → ExpenseEntity with all rich fields (amount, category, location, person, rawInput, transactionDate). Update ALL repository references, service references, and the pipeline.
Step 1.5 — Add spring-boot-starter-validation to pom.xml and add @Valid to all request DTOs with @NotBlank, @NotNull annotations.
Deliverable: POST /api/v1/upload/pdf accepts a PDF, extracts text, runs it through DeepSeek extraction, returns structured JSON. Single unified ExpenseEntity.
---
Sprint 2 (Week 8) — Romanian NER Edge Cases + Validation
Step 2.1 — Enhance the ExtractionAssistant system prompt to handle Romanian edge cases:
"o sută jumate" → 150.00
"alaltăieri" → computed LocalDate
"2 milioane" → 2000000
"ieri", "alaltăieri", "poimâine" → relative date calculation
Add @UserMessage template with examples (few-shot prompting).
Step 2.2 — Add CurrencyNormalizer in com.proiect.util:
public class CurrencyNormalizer {
    public static BigDecimal parseRomanianAmount(String raw) { ... }
    public static String detectCurrency(String text) { ... }  // RON/EUR/USD from context
}
Step 2.3 — Add DateNormalizer in com.proiect.util:
public class DateNormalizer {
    public static LocalDate resolveRelativeDate(String romanianExpr) { ... }
}
Map "astăzi", "ieri", "alaltăieri", "săptămâna trecută", etc.
Step 2.4 — Create ExtractionServiceTest v2: test all Romanian edge cases with mocked LLM responses (do NOT call real API in unit tests).
Deliverable: DeepSeek correctly extracts amounts like "o sută jumate" → 150.00, relative dates → computed LocalDate, currency detection → RON/EUR/USD.
---
Sprint 3 (Week 9) — OCR Stub + Pipeline Hardening
Step 3.1 — Create OcrExtractionService stub (Tesseract can be added later, for now use LLM-based image description):
@Service @Slf4j @RequiredArgsConstructor
public class OcrExtractionService {
    private final ChatLanguageModel chatLanguageModel;
    public String extractTextFromImage(MultipartFile image) {
        // TODO: Tess4J integration. For now, throw UnsupportedOperationException
        // with message "OCR not yet available — use PDF upload"
    }
}
Step 3.2 — Add retry logic to ExtractionService: if DeepSeek returns malformed JSON, retry up to 2 times with exponential backoff (2s, 4s). Use Spring Retry or manual loop.
Step 3.3 — Fix GlobalExceptionHandler to handle PipelineException and generic RuntimeException (not just AmountNotFoundException).
Deliverable: Robust extraction pipeline with retry, proper error handling, OCR endpoint stub returning 501.
---
ALEXIA — Embeddings & RAG Retrieval
Sprint 1 (Week 7) — Real Embeddings + Qdrant Client
Step 1.1 — Add LangChain4j OpenAI embedding dependency to pom.xml:
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.31.0</version>
</dependency>
(already present, just need to use OpenAiEmbeddingModel)
Step 1.2 — Create EmbeddingConfig in com.proiect.config:
@Configuration
public class EmbeddingConfig {
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
            .apiKey(System.getenv("DEEPSEEK_API_KEY"))
            .modelName("text-embedding-3-small")  // or use DeepSeek's embedding model
            .dimensions(1536)
            .build();
    }
}
If DeepSeek doesn't offer embeddings yet, use the OpenAI key with text-embedding-3-small.
Step 1.3 — Rewrite EmbeddingService to use the real model:
@Service @Slf4j @RequiredArgsConstructor
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    public float[] getEmbedding(String text) {
        Response<Embedding> response = embeddingModel.embed(text);
        return response.content().vector();
    }
}
Step 1.4 — Delete the raw-REST ExpenseVectorRepositoryImpl. Replace with LangChain4j's Qdrant integration:
Add to pom.xml:
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-qdrant</artifactId>
    <version>0.31.0</version>
</dependency>
Step 1.5 — Create QdrantConfig in com.proiect.config:
@Configuration
public class QdrantConfig {
    @Bean
    public QdrantStore qdrantStore() {
        return QdrantStore.builder()
            .host("localhost")
            .port(6334)  // gRPC port
            .collectionName("expenses")
            .dimension(1536)
            .build();
    }
}
Step 1.6 — Delete ExpenseVectorRepositoryImpl and ExpenseVectorRepository interface. Create QdrantVectorService in com.proiect.service:
@Service @Slf4j @RequiredArgsConstructor
public class QdrantVectorService {
    private final QdrantStore qdrantStore;
    private final EmbeddingService embeddingService;
    public void storeExpense(ExpenseEntity expense) { ... }
    public List<EmbeddedExpense> searchSimilar(String query, int topK) { ... }
    public boolean exists(Long expenseId) { ... }
}
Step 1.7 — Recreate the Qdrant collection with dimension 1536 (delete old size: 10 collection first):
curl -X DELETE 'http://localhost:6333/collections/expenses'
curl -X PUT 'http://localhost:6333/collections/expenses' \
  -H 'Content-Type: application/json' \
  -d '{"vectors": {"size": 1536, "distance": "Cosine"}}'
Deliverable: Real 1536-dim embeddings stored in Qdrant. QdrantVectorService replaces the old raw-REST impl.
---
Sprint 2 (Week 8) — Vector Search + Hybrid Query
Step 2.1 — Implement searchSimilar() in QdrantVectorService:
public List<EmbeddedExpense> searchSimilar(String query, int topK) {
    float[] queryVector = embeddingService.getEmbedding(query);
    // Use Qdrant client search with cosine similarity
    // Return top-K results with payload (entityId, category, person, date)
}
Step 2.2 — Create ExpenseSearchController in com.proiect.controller:
@RestController @RequestMapping("/api/v1/search") @Slf4j @RequiredArgsConstructor
public class ExpenseSearchController {
    @PostMapping           // semantic search: "cafea luni" → top-5 similar expenses
    @PostMapping("/filter") // hybrid: vector search + metadata filters (category=Rent, person=Maria)
}
Step 2.3 — Fix SyncService to actually run on every new expense. Wire it into ExpensePipelineService:
- After saving to PostgreSQL → call QdrantVectorService.storeExpense()
- Delete the random-dummy-vector code from ExpensePipelineService
- Use EmbeddingService.getEmbedding() to generate real vectors
Step 2.4 — Add metadata filtering to Qdrant search: filter by category, person, location, transactionDate range. Use Qdrant's payload filtering in the search request.
Deliverable: POST /api/v1/search returns semantically similar expenses. POST /api/v1/search/filter returns filtered results. All new expenses auto-sync to Qdrant with real embeddings.
---
Sprint 3 (Week 9) — RAG Retrieval Pipeline
Step 3.1 — Create RagRetrievalService in com.proiect.service:
@Service @Slf4j @RequiredArgsConstructor
public class RagRetrievalService {
    private final QdrantVectorService qdrantVectorService;
    public String retrieveContext(String query) {
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(query, 5);
        // Format results into a context string for the LLM prompt
        // "Previous expenses: 1. Coffee 15 RON at Starbucks on 2024-03-15..."
        return formattedContext;
    }
}
Step 3.2 — Create RagQueryController in com.proiect.controller:
@PostMapping("/api/v1/rag/query")
// User asks: "Cât am cheltuit pe cafea luna asta?"
// 1. RAG fetches context from Qdrant
// 2. Injects context into LLM prompt
// 3. LLM answers with grounded data
Step 3.3 — Modify ExtractionAssistant system prompt to include retrieved context:
@SystemMessage("""
You are a family expense assistant. Answer questions about expenses using ONLY the provided context.
Context: {{context}}
If the context doesn't contain enough information, say "Nu am suficiente date."
""")
Step 3.4 — Add Content (LangChain4j) integration for RAG. Use RetrievalAugmentor:
@Bean
public RetrievalAugmentor retrievalAugmentor(QdrantStore qdrantStore, EmbeddingModel embeddingModel) {
    EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(qdrantStore)
        .embeddingModel(embeddingModel)
        .maxResults(5)
        .build();
    return DefaultRetrievalAugmentor.builder()
        .contentRetriever(retriever)
        .build();
}
Deliverable: Working RAG pipeline. POST /api/v1/rag/query returns grounded answers from Qdrant context.
---
LAURA — AI Analytics & Function Calling
Sprint 1 (Week 7) — Expense Query API + CRUD
Step 1.1 — Create ExpenseController (basic CRUD — needed for everything else to work):
@RestController @RequestMapping("/api/v1/expenses") @Slf4j @RequiredArgsConstructor
public class ExpenseController {
    @GetMapping            // list all expenses (paginated)
    @GetMapping("/{id}")   // get single expense
    @GetMapping("/by-category/{cat}") // filter by category
    @GetMapping("/by-person/{person}") // filter by person
    @GetMapping("/by-date-range")     // ?from=2024-01-01&to=2024-03-31
}
Step 1.2 — Add pagination support: use Pageable in repository, return Page<ExpenseEntity>.
Step 1.3 — Create ExpenseAnalyticsService in com.proiect.service:
@Service @Slf4j @RequiredArgsConstructor
public class ExpenseAnalyticsService {
    private final ExpenseJpaRepository repository;
    public BigDecimal calculateTotal(LocalDate from, LocalDate to) { ... }
    public Map<String, BigDecimal> compareMembers(LocalDate from, LocalDate to) { ... }
    public Map<String, BigDecimal> byCategory(LocalDate from, LocalDate to) { ... }
    public List<ExpenseEntity> detectAnomalies(BigDecimal threshold) { ... }
}
Deliverable: Full expense CRUD API. Analytics methods compute real totals from PostgreSQL.
---
Sprint 2 (Week 8) — Function Calling with @Tool
Step 2.1 — Create ExpenseTools in com.proiect.service:
public class ExpenseTools {
    @Tool("Calculate total expenses for a date range")
    public String calculateTotal(String from, String to) {
        BigDecimal total = analyticsService.calculateTotal(
            LocalDate.parse(from), LocalDate.parse(to));
        return "Total: " + total + " RON";
    }
    @Tool("Compare spending between family members")
    public String compareMembers(String from, String to) { ... }
    @Tool("Detect spending anomalies above a threshold")
    public String detectAnomalies(String thresholdStr) { ... }
    @Tool("Get expense breakdown by category")
    public String byCategory(String from, String to) { ... }
    @Tool("Get expenses for a specific person")
    public String byPerson(String person, String from, String to) { ... }
    @Tool("Compare two time periods")
    public String comparePeriods(String from1, String to1, String from2, String to2) { ... }
    @Tool("Get top N highest expenses")
    public String topExpenses(String limit) { ... }
    @Tool("Calculate monthly average spending")
    public String monthlyAverage(String months) { ... }
}
Step 2.2 — Create AnalyticsController:
@PostMapping("/api/v1/analytics/query")
// User: "Cât am cheltuit total în martie?"
// LLM decides to call calculateTotal("2024-03-01", "2024-03-31")
// Returns grounded answer: "Total martie: 2,450 RON"
Step 2.3 — Register the tools with LangChain4j AiServices:
ExtractionAssistant analyticsAssistant = AiServices.builder(ExtractionAssistant.class)
    .chatLanguageModel(chatLanguageModel)
    .tools(new ExpenseTools(analyticsService))
    .build();
Deliverable: POST /api/v1/analytics/query answers questions using calculator tools — no numeric hallucination.
---
Sprint 3 (Week 9) — Report Generation + Hallucination Guard
Step 3.1 — Create ReportService in com.proiect.service:
@Service @Slf4j @RequiredArgsConstructor
public class ReportService {
    public String generateMonthlySummary(int year, int month) { ... }
    // LLM summarizes: "Această lună ați cheltuit cu 15% mai mult pe divertisment"
}
Step 3.2 — Create HallucinationGuard:
@Component @Slf4j @RequiredArgsConstructor
public class HallucinationGuard {
    private final ExpenseAnalyticsService analyticsService;
    public String validate(String llmOutput, String toolOutput) {
        // If LLM says "total 5000" but tool says "total 4500", flag it
        // Return corrected version or warning
    }
}
Step 3.3 — Generate text descriptions for trending data (consumed by frontend later):
@Tool("Describe spending trend for a category")
public String describeTrend(String category, String from, String to) { ... }
Deliverable: Monthly summaries, hallucination-checked outputs, trend descriptions.
---
TEODOR — Testing, Integration, CI/CD, Glue
Sprint 1 (Week 7) — Fix Foundation + CI Pipeline
Step T7.1 — Unify entity model (review Dumitrița's Step 1.4 PR). Verify all references updated.
Step T7.2 — Fix GlobalExceptionHandler to handle ALL exception types:
@ExceptionHandler(PipelineException.class) → 500
@ExceptionHandler(AmountNotFoundException.class) → 422
@ExceptionHandler(MethodArgumentNotValidException.class) → 400
@ExceptionHandler(Exception.class) → 500 (catch-all)
Step T7.3 — Fix ExpenseVectorRepositoryImpl → replace System.out.println with log.warn.
Step T7.4 — Fix VectorController to depend on the interface, not the impl class.
Step T7.5 — Set up GitHub Actions CI (.github/workflows/ci.yml):
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./mvnw clean compile
      - run: ./mvnw test
Set DEEPSEEK_API_KEY and OPENROUTER_API_KEY as GitHub Secrets.
Step T7.6 — Mark ApiConnectivityTest with @Disabled or @Tag("integration") so it doesn't run in CI (it makes real API calls).
Step T7.7 — Write test for PdfExtractionService (Unit test with a small sample PDF in src/test/resources/).
Step T7.8 — Write test for GlobalExceptionHandler (verify HTTP status codes for each exception type).
Deliverable: CI pipeline green. No compiler warnings. Clean exception handling. Entity model unified.
---
Sprint 2 (Week 8) — Integration Tests + End-to-End
Step T8.1 — Write integration test for the full pipeline:
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class PipelineIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = ...;
    @Container static GenericContainer<?> qdrant = ...;
    @Test void fullPipeline_fromPdfToSearch() { ... }
}
Add Testcontainers dependencies to pom.xml.
Step T8.2 — Write test for QdrantVectorService.searchSimilar():
- Mock the Qdrant client response
- Verify results are ranked by score
- Verify metadata filters are applied
Step T8.3 — Write test for ExpenseTools.calculateTotal():
- Mock repository returning known amounts
- Verify the tool returns the correct total string
Step T8.4 — Write test for RagRetrievalService:
- Mock QdrantVectorService.searchSimilar() 
- Verify context string formatting
- Verify prompt injection
Step T8.5 — Wire everything together: after reviewing PRs from Week 8, update ExpensePipelineService to use:
- EmbeddingService (real) instead of random vectors
- QdrantVectorService instead of raw REST
- SyncService triggered after every save
Step T8.6 — Add README.md update with new endpoints and setup instructions.
Deliverable: Integration tests pass. Full pipeline: PDF → extract → store → embed → search → RAG → respond, all wired together.
---
Sprint 3 (Week 9) — Polish + Demo
Step T9.1 — Write contract tests for ALL endpoints:
@Test void uploadPdf_returnsStructuredExtraction()
@Test void searchSemantically_returnsTopK()
@Test void analyticsQuery_usesToolsNoHallucination() 
@Test void ragQuery_groundsAnswerInContext()
Step T9.2 — Fix OpenRouter fallback: either replace with a working free model (e.g., mistralai/mistral-7b-instruct:free) or remove the bean and throw IllegalStateException immediately with a clear message.
Step T9.3 — Add a health check endpoint:
@GetMapping("/api/v1/health")
// Returns: { postgresql: "UP", qdrant: "UP", deepseek: "UP" }
Step T9.4 — Add docker-compose.yml persistence volumes:
postgres:
  volumes: pgdata:/var/lib/postgresql/data
qdrant:
  volumes: qdrantdata:/qdrant/storage
Step T9.5 — Final review: ensure all controller responses use proper DTOs (not raw String). Ensure all services have @Transactional where needed. Ensure no System.out anywhere.
Step T9.6 — Demo rehearsal: run docker-compose up -d, start the app, run through:
1. Upload PDF → extracted expense
2. Semantic search → relevant results
3. RAG query → grounded answer
4. Analytics query → tool-computed answer
5. Error scenarios → proper HTTP responses
Deliverable: Demo-ready application at ~55% completion. CI green. All endpoints functional.
---
DEPENDENCY MAP (Who blocks whom)
Week 7:
  Dumitrița (PDF upload, entity unification) ──→ Teodor (fix references, tests)
  Alexia (real embeddings, Qdrant client) ─────→ Teodor (integration test needs QdrantService)
Week 8:
  Dumitrița (NER edge cases)  ──→ Alexia (RAG retrieval uses extraction prompts)
  Alexia (vector search, RAG) ──→ Laura (analytics needs search context)
  Laura (CRUD, analytics calc)  ──→ Alexia (sync needs repository)
  Everyone ───────────────────→ Teodor (integration + wiring)
Week 9:
  Laura (tools, hallucination guard) ──→ Teodor (contract tests)
  Everyone ────────────────────────────→ Teodor (polish, demo, push to main)
WHAT 55% LOOKS LIKE
Feature	Target %	Reality at Week 9
PDF upload & extraction	100%	✅ PDFBox → DeepSeek NER → PostgreSQL
Real embeddings	100%	✅ OpenAI/DeepSeek 1536-dim
Vector search	100%	✅ Cosine similarity + metadata filters
RAG pipeline	80%	✅ Retrieval + grounding (no re-ranking yet)
Function Calling	80%	✅ 8 tools, hallucination guard
Voice/OCR	0%	❌ Stub only (Week 10+)
Conversational Memory	0%	❌ (Week 10+)
Cost Tracking	0%	❌ (Week 10+)
Report PDF export	0%	❌ (Week 10+)
Frontend	0%	❌ Separate repo