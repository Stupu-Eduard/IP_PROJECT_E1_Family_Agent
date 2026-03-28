# MASTER DOCUMENT: FAMILY AGENT PROJECT

## 1. GLOBAL PROJECT ARCHITECTURE (6 MODULES)

**M1 - DB TEAM (PostgreSQL Core):** Defines the master schema, migrations (Flyway), and DB performance.

**M2 - OCR TEAM (Receipt Processing):** Tesseract/Google Vision integration to turn receipt images into raw text.

**M3 - AI API TEAM (The Brain - MY TEAM):**
*   **Dumitrița:** Extraction (Regex/LLM) - Text to JSON.
*   **Alexia:** Persistence & Vector Store (PostgreSQL + Qdrant).
*   **Laura (ME):** Orchestration (ETL Pipeline) & AI Analytics (Claude Tools).

**M4 - DEVOPS TEAM:** Docker, CI/CD, Cloud Hosting, and Security.

**M5 - UI TEAM (Frontend):** React/Mobile app for user interaction and dashboards.

**M6 - GOOGLE MAPS API:** Location tracking and spending heatmaps.

## 2. MY TEAM (M3 - AI API) DETAILED WORKFLOW

**Input:** Raw text (from UI) or OCR text (from M2).

**Process:**
1.  Laura's Pipeline starts.
2.  Calls Dumitrița's Extraction API.
3.  Receives Structured DTO.
4.  Saves to M1's PostgreSQL.
5.  Saves to Alexia's Qdrant (Vector DB).

**Output:** Confirmed ID and AI-generated insights for M5 (UI).

## 3. MY TASKS FOR THIS WEEK (LAURA - M3)

**Goal:** Build the "Integration & Data Pipeline".

**Components:** `ExtractionClient` (REST), `ExpensePipelineService` (@Transactional), `PipelineController`, `PipelineValidationService`.

**Tech:** Java 21, Spring Boot 3, RestTemplate.
