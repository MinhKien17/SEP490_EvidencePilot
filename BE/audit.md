# Audit: Evidence Pilot Backend

*Last updated: 2026-07-18*

---

## Requirements Coverage

### Student Functional Requirements

| # | Requirement | Status | Evidence |
|---|-------------|--------|----------|
| 1 | Submit project materials / upload sources | ✅ DONE | `DocumentController` upload, MinIO storage, project CRUD |
| 2 | Review extracted metadata & candidate evidence | ✅ DONE | `DocumentText`, `DocumentChunk`, `PaperSection`, `DocumentReference` endpoints |
| 3 | Evaluate evidence strength | ✅ DONE | `ClaimEvaluationService`, `EvidenceEdge` with `confidence_score`, `AiSuggestion.score` |
| 4 | Check active links (URL accessibility) | ❌ **MISSING** | No link‑checking service or endpoint found anywhere in codebase |
| 5 | Create / refine claim–source mappings | ✅ DONE | `ClaimEvidenceMapping`, `ClaimController` PUT/claim |
| 6 | View AI suggestions; accept/reject | ✅ DONE | `AiSuggestion` with PENDING/ACCEPTED/REJECTED/INVALIDATED, PATCH `suggestions/{id}/status` |
| 7 | Visualize relationships (papers ↔ sources) | ✅ DONE | `EvidenceEdge` graph, evidence‑mapping queries |
| 8 | Inspect unsupported‑claim / weak‑source warnings | ✅ DONE | `TraceabilityExportService` flags unlinked claims |
| 9 | Export evidence trace summaries | ✅ DONE | `TraceabilityExportController` GET `/projects/{id}/traceability/export` |

### Instructor / Reviewer Functional Requirements

| # | Requirement | Status | Evidence |
|---|-------------|--------|----------|
| 1 | View project evidence map & source trace | ✅ DONE | Project documents + claims endpoints |
| 2 | Inspect unsupported claims & review flags | ✅ DONE | `TraceabilityExportService` |
| 3 | Review evidence coverage summaries | ✅ DONE | Traceability export includes coverage |
| 4 | Add comments / supervision notes | ✅ DONE | `SectionFeedback`, `InstructorFeedback`, `FeedbackRequest` |
| 5 | Use traceability views | ✅ DONE | Dedicated controller + service |

### AI Evidence Engine

| # | Requirement | Status | Evidence |
|---|-------------|--------|----------|
| 1 | LLM Extraction (metadata + snippets) | ✅ DONE | `AiAnalysisService`, `AiModelClient`, extraction pipeline |
| 2 | Correlation Marking (claim ↔ source) | ✅ DONE | `ClaimMatchingService`, `ClaimEvaluationService` |
| 3 | Rules‑Based Scoring | ✅ DONE | `ClaimEvaluationService` deterministic scorer |
| 4 | Gap Detection (unlinked / low‑scoring claims) | ✅ DONE | `TraceabilityExportService` |
| 5 | API Delivery (JSON endpoints) | ✅ DONE | All REST controllers |

### System Administrator

| # | Requirement | Status | Evidence |
|---|-------------|--------|----------|
| 1 | Manage users, roles, permissions | ✅ DONE | `UserController`, `ProjectMember`, JWT RBAC |
| 2 | Monitor storage, logs, config | ⚠️ PARTIAL | `HealthController` (basic), no admin dashboard |
| 3 | Security, backup, availability | ✅ DONE | Docker Compose, JWT, role isolation |

### Non‑Functional Requirements

| # | Requirement | Status | Notes |
|---|-------------|--------|-------|
| 1 | Performance | ✅ COVERED | Async extraction via RabbitMQ, chunked processing |
| 2 | Usability (simple workflows) | ✅ COVERED | Well‑structured REST API |
| 3 | Security (RBAC, file handling) | ✅ COVERED | JWT auth, workspace isolation via `CurrentUserService`, MinIO signed URLs |
| 4 | Reliability (reproducible evidence) | ✅ COVERED | Status tracking, audit trail, extraction retry |
| 5 | Transparency (AI editable) | ✅ COVERED | All AI output is PENDING; user must explicitly accept |

---

## Previously Open Issues — Re‑assessment

| # | Severity | Location | Issue | Previous | Current |
|---|----------|----------|-------|----------|---------|
| 1 | HIGH | `Document.java` | Missing `doi` field | OPEN | ✅ **FIXED** (lines 78‑79) |
| 2a | HIGH | `DocumentServiceImpl.java:192‑254` | `uploadDocument` never calls `refreshProjectStatus` | OPEN | ✅ **FIXED** (lines 254‑256) |
| 2b | HIGH | `SourceExtractionServiceImpl.java:36` | RabbitMQ payload is bare UUID | OPEN | ⚠️ **PARTIAL** — `ExtractionRequest` record exists but still only holds `documentId`. S3 key & userId not sent. Worker reloads document from DB so functional, but sub‑optimal. |
| 3a | MEDIUM | `application.yml:10‑11` | DB_HOST / DB_PORT / DB_NAME / DB_USERNAME have fallbacks | OPEN | ❌ **STILL OPEN** — `url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:evidence_pilot}` and `username: ${DB_USERNAME:root}` |
| 3b | MEDIUM | `application.yml:136` | MINIO_URL fallback | FIXED | ✅ **CONFIRMED FIXED** |

---

## New Findings

### N6. Missing: Active Link Checking — **HIGH**

**Requirement:** *"Check active links: Our system verifies whether referenced URLs and paper links are still accessible."*

**Reality:** Zero implementation — no service, endpoint, or scheduled task for URL accessibility checking exists in the codebase.

### N7. Schema Drift — `document_references` table — **MEDIUM**

**File:** `schema.sql:120‑128`

The `document_references` DDL is missing two columns that exist in the entity (`DocumentReference.java`):
- `doi VARCHAR(255)` (entity line 36)
- `edge_type VARCHAR(50)` (entity lines 38‑40)

Hibernate `ddl-auto: update` will add them, but `schema.sql` (used as `docker-entrypoint-initdb.d`) is stale. This causes a mismatch during fresh Docker deployments until Hibernate applies its own migration.

### N8. No Flyway Migrations — **MEDIUM**

**Reality:** Flyway is disabled (`flyway.enabled: false`, no `db/migration/` directory). Schema management relies on:
1. `schema.sql` mounted as `docker‑entrypoint‑initdb.d/01_schema.sql`
2. Hibernate `ddl‑auto: update`
3. `SchemaMigrationRunner` (Java `CommandLineRunner`) for processing_status ENUM→VARCHAR migration

This is functional but unconventional. Schema changes must be tracked manually.

### N9. `application.yml` Excludes SecurityAutoConfiguration — **LOW**

**Lines 4‑7:** `spring.autoconfigure.exclude` lists both `SecurityAutoConfiguration` and `UserDetailsServiceAutoConfiguration`. However, `SecurityConfig.java` provides a full `SecurityFilterChain` bean with JWT filter. The exclusion is intentional (to avoid double‑configuration) but unusual. This works correctly but could confuse future maintainers.

### N10. Document References Table — No `reference_url` Column — **LOW**

**Requirement context:** Link checking would need a `reference_url` or `source_url` column on references to know *what* to check. Neither `schema.sql` nor the entity stores the original URL of a reference; only `raw_text` (full citation text) is stored.

---

## Summary

| # | Severity | Area | Issue | Status |
|---|----------|------|-------|--------|
| 1 | HIGH | Functional | **Active link checking** not implemented | **OPEN** |
| 2 | MEDIUM | Config | DB_HOST/DB_PORT/DB_NAME/DB_USERNAME fallbacks in `application.yml` | **OPEN** |
| 3 | MEDIUM | Schema | `document_references` table missing `doi` and `edge_type` columns | **OPEN** |
| 4 | MEDIUM | Infra | No Flyway migration files; schema managed ad‑hoc | **OPEN** |
| 5 | LOW | Config | SecurityAutoConfiguration exclusion may confuse | **INFO** |
| 6 | LOW | Schema | Missing `reference_url` column needed for link‑checking feature | **INFO** |
| P1 | HIGH | Entity | DOI field on Document.java | **FIXED** |
| P2a | HIGH | Service | uploadDocument refreshProjectStatus | **FIXED** |
| P2b | HIGH | Service | RabbitMQ payload bare UUID (partial fix) | **PARTIAL** |
| P3b | MEDIUM | Config | MINIO_URL fallback | **FIXED** |
| N1‑N5 | MED/LOW | Various | Previous audit items | **FIXED** |
