# Context Proiect: Family Agent - Modul M3

Acest fișier servește drept fundament pentru instrucțiunile și convențiile specifice modulului M3 (Extracție AI și Normalizare). Orice interacțiune ulterioară trebuie să respecte detaliile de mai jos.

## 1. Rezumat Proiect (Family Agent)
**Sistem inteligent de monitorizare a cheltuielilor familiale.**
*   **Clasificare automată:** Categorii (mâncare, transport, divertisment) și locații.
*   **NLP:** Interfață de interogare în limbaj natural cu răspunsuri textuale și grafice.
*   **Persistență:** Datele sunt salvate într-o bază de date PostgreSQL.

## 2. Structura Echipei și Task-uri
*   **M3 (Dumitrița - Lead Modul):** Extracție AI (NER) și Normalizare. Transformă inputul nestructurat în date validate pentru baza de date.
*   **Alexia:** Vector Store (Qdrant) și Embeddings pentru căutare semantică.
*   **Teodor:** Orchestrare LLM (DeepSeek/Claude) și managementul memoriei în PostgreSQL.
*   **Laura:** Analiză de date și generare de rapoarte vizuale folosind unelte Java (@Tool).

## 3. Detalii Implementare M3 (Task-ul curent)
*   **Stack Tehnic:** Java 21, Spring Boot 3.x, Spring Data JPA, PostgreSQL.
*   **AI Logic:** Integrare **LangChain4j** cu **DeepSeek V3.2** pentru recunoașterea entităților (sumă, persoană, categorie, locație).
*   **Logică de Normalizare:** 
    *   Conversie text -> cifre (ex: "o sută jumate" -> `150.0`).
    *   Calcul date relative (ex: "alaltăieri" -> calculat din data curentă).
*   **OCR & Voice:** Tesseract (bonuri) și Whisper (transcriere audio).

## 4. Arhitectură și API (M3)
*   **Model:** `ExpenseEntity` mapat pe tabelul `expenses`.
*   **Endpoint:** `POST /api/v1/extract` (preia text brut, returnează DTO JSON).
*   **Flux:** Controller -> Service (AI Logic) -> Repository (PostgreSQL).

### Entitatea JPA (Reference)
```java
@Entity
@Table(name = "expenses")
public class ExpenseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal amount;
    private String category;
    private String location;
    private String person;
    private LocalDateTime transactionDate;
    private String rawInput;
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### Configurare application.yml (PostgreSQL)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/family_agent_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:admin}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

langchain4j:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
```

## 5. Convenții de Lucru
*   **Branch Principal:** `M3`. Toate modificările se fac prin branch-uri `feature/nume-functie` cu merge în `M3`.
*   **Workflow Gemini CLI:** Atunci când asisti la acest proiect, asigură-te că orice cod generat respectă structura de entitate de mai sus și utilizează LangChain4j pentru partea de AI.
