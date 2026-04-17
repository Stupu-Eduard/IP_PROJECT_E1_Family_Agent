# AGENTS.md — Family Agent

## Build & Run Commands

| Command | Description |
|---|---|
| `./mvnw clean compile` | Compile the project |
| `./mvnw spring-boot:run` | Run the Spring Boot application |
| `./mvnw test` | Run all tests |
| `./mvnw test -Dtest=ClassName` | Run a single test class |
| `./mvnw test -Dtest="ExtractionServiceTest#methodName"` | Run a single test method |
| `./mvnw test -Dtest="com.proiect.service.*"` | Run tests in a package |
| `./mvnw clean package` | Build the JAR (skips tests with `-DskipTests`) |
| `docker-compose up -d` | Start Postgres + Qdrant infrastructure |

**Runtime:** Java 21, Maven 3.9.5 (via wrapper), Spring Boot 3.2.4

## Infrastructure

- **Database (default profile):** H2 in-memory at `jdbc:h2:mem:family_agent`, console at `/h2-console`
- **Database (Docker):** PostgreSQL 16 on `localhost:5432` (user: `user`, password: `password`, db: `family_agent`)
- **Vector Store:** Qdrant on `localhost:6333`, collection `expenses`
- **LLMs:** DeepSeek (`deepseek-chat`) and OpenRouter (`nvidia/nemotron-4-340b-instruct`) via LangChain4j 0.31.0

## Project Structure

```
src/main/java/com/proiect/
├── FamilyAgentApplication.java
├── config/          — Spring @Configuration (CORS, LLM beans, RestTemplate)
├── controller/      — REST controllers (/api/v1/*)
├── dto/             — Request/response DTOs (@Data classes and Java records)
├── exception/       — Custom exceptions + @ControllerAdvice handler
├── model/           — JPA @Entity classes
├── repository/      — Spring Data JPA interfaces + custom Qdrant impl
├── service/         — Business logic (extraction, pipeline, sync, validation)
└── util/            — Normalization utilities
```

Layered architecture: Controller → Service → Repository. No circular dependencies between layers.

## Testing

- **Framework:** JUnit 5 + Mockito via `spring-boot-starter-test`
- **Pattern:** `@ExtendWith(MockitoExtension.class)` with `@Mock` / `@InjectMocks`
- **Assertions:** Static imports from `org.junit.jupiter.api.Assertions.*`
- **Test naming:** `{ClassUnderTest}Test`
- **No Spring context tests** — all unit tests use plain Mockito; `ApiConnectivityTest` makes real API calls

## Code Style

### Imports

- Explicit imports only — **no wildcard imports**
- Static imports allowed in test files (Assertions, Mockito matchers)
- Lombok annotations imported explicitly

### Formatting

- **Indentation:** 4 spaces (no tabs)
- **Braces:** K&R style (opening brace on same line)
- **Blank lines:** One between methods; blank line after package declaration
- **No enforced line length** (no linter configured)

### Naming

| Element | Convention | Example |
|---|---|---|
| Packages | lowercase | `com.proiect.service` |
| Classes | PascalCase | `ExtractionService`, `PipelineController` |
| Interfaces | PascalCase (no prefix) | `ExpenseVectorRepository` |
| Methods | camelCase | `processRawInput`, `validatePersistence` |
| Variables | camelCase | `rawText`, `chatLanguageModel` |
| Constants | UPPER_SNAKE_CASE | `QDRANT_URL` |
| Test classes | `{Class}Test` | `ExtractionServiceTest` |

### Types & Generics

- Use `BigDecimal` for monetary values — never `double` or `float`
- Use `java.time.LocalDate` / `LocalDateTime` — never `java.util.Date`
- Use Java `record` for simple immutable DTOs; use `@Data` + `@Builder` for mutable DTOs
- **Do not use `var`** — always declare explicit types (current codebase convention)
- Generics used with standard Spring patterns (`JpaRepository<T, ID>`, `ResponseEntity<T>`)

### Annotations

- **DI:** Prefer constructor injection via `@RequiredArgsConstructor` (Lombok). Avoid `@Autowired` field injection.
- **Controllers:** `@RestController` + `@RequestMapping` on class, `@PostMapping`/`@GetMapping` on methods
- **Services:** `@Service` + `@Slf4j` + `@RequiredArgsConstructor`
- **Entities:** `@Entity`, `@Table`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- **Transactional methods:** `@Transactional` on service methods that persist data
- **LangChain4j:** `@SystemMessage`, `@UserMessage`, `@V` for prompt templates

### Error Handling

- Throw **unchecked exceptions** — do not use checked exceptions
- Custom exceptions extend `RuntimeException`:
  - `PipelineException` — generic pipeline failure
  - `AmountNotFoundException` — annotated `@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)`
- `GlobalExceptionHandler` (`@ControllerAdvice`) converts exceptions to `Map<String, Object>` responses with `timestamp`, `message`, `status`
- **Never swallow exceptions silently** — log them and/or rethrow

### Logging

- Use Lombok `@Slf4j` — never create loggers manually
- Parameterized messages: `log.info("Processing id: {}", id)`
- Exception logging: `log.error("Description", exception)`
- Levels: `log.info` for flow, `log.warn` for non-fatal issues, `log.error` for failures
- **No `System.out.println`** in production code

### Lombok Usage

- `@Slf4j` on all services, controllers, and config classes
- `@RequiredArgsConstructor` for constructor injection (preferred over `@Autowired`)
- `@Data` + `@Builder` for mutable DTOs; `record` for immutable DTOs
- `@Getter` / `@Setter` / `@NoArgsConstructor` on entities when `@Data` is too broad
- Avoid `@Data` on JPA `@Entity` classes — use `@Getter` + `@Setter` + `@NoArgsConstructor` instead (to prevent `equals`/`hashCode` issues with lazy-loaded associations)

## Key Libraries

- **LangChain4j 0.31.0** — LLM integration (chat models, prompt templates, AI services)
- **Spring Data JPA** — relational data access
- **Spring Web** — REST controllers
- **PostgreSQL** (runtime) + **H2** (default/test) — databases
- **Lombok** — boilerplate reduction (annotation processing configured in `maven-compiler-plugin`)

## Environment

API keys loaded from environment variables (`.env` file):
- `DEEPSEEK_API_KEY` — DeepSeek LLM access
- `OPENROUTER_API_KEY` — OpenRouter LLM access (fallback provider)

LLM fallback chain: DeepSeek → OpenRouter → `IllegalStateException`