# SEP490 EvidencePilot — Architectural Analysis

## 1. Core System Objective

EvidencePilot is a capstone thesis evidence-management platform for FPT University SEP490 students. Students author a LaTeX paper section-by-section, extract discrete "claims" from their writing, and prove each claim with evidence excerpts from uploaded source PDFs — with an AI pipeline (MinerU extraction → vector embedding → RAG claim matching → LLM evaluation) automating the grunt work. Instructors curate source collections, review claim-evidence mappings, and run a bidirectional review loop (feedback → student answers → re-submit → approve); an admin console manages users, broadcasts notifications, and audits the trail.

## 2. Tech Stack & Infrastructure

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5 (Web, Data JPA, Security, AMQP, WebSocket, Mail, Validation), Lombok, MapStruct 1.5.5, jjwt 0.12.6, MinIO SDK 8.5.11, Guava, Springdoc 2.5.0 |
| Frontend | React 19 + Vite 8, Tailwind 4 (`@tailwindcss/vite`), react-router-dom 7, axios, i18next (EN/VI), CodeMirror 6 + `codemirror-lang-latex`, KaTeX, vis-network, STOMP.js, driver.js, @hello-pangea/dnd. No tests, no lint. |
| Databases | MySQL 8.0.46 (relational, Hibernate `ddl-auto=update` — Flyway **disabled**); Qdrant (vector, collection `source_chunks`, dense 768-d cosine + sparse IDF vectors) |
| Infra | Docker Compose: MySQL, Qdrant (+`qdrant-init` curl bootstrap), MinIO (+`mc` bucket init), RabbitMQ 3.13, backend image; RabbitMQ used for extraction + export queues (3 retry attempts, 5s→30s backoff, `default-requeue-rejected=false`) |
| External services | Python AI worker (`/extract`, `/ai/generate`, `/ai/embeddings[/batch]`; Ollama `nomic-embed-text` / `evidencopilot:latest`), OpenAlex REST (DOI metadata), SMTP, MinIO presigned URLs |
| Deployment | Dockerized BE behind `127.0.0.1` ports; FE on Vercel (`sep-490-prototype.vercel.app` in CORS allowlist); ngrok tunnels for BE exposure |

## 3. Core Data Architecture & Entity Relationships

20 JPA entities mirroring `schema.sql` (13 table groups). All PKs are `BINARY(16)` UUIDs.

**Core graph:**

- `users` → `projects` (via `project_members`: role `LEADER/MEMBER/INSTRUCTOR`, UNIQUE(project_id, user_id)) — users carry `role` (`STUDENT/INSTRUCTOR/ADMIN`), `account_status` (`PENDING/ACTIVE/BANNED/DELETED`), `email_verified`, hashed verification/reset tokens, and a `token_version` counter used for JWT invalidation.
- `projects` (status machine `CREATED → ASSIGNED → IN_PROGRESS → SUBMITTED_FOR_REVIEW → RETURNED/APPROVED/ARCHIVED`; `APPROVED/ARCHIVED` are read-only) → `documents` (`doc_type PAPER|SOURCE`, 11-state `processing_status`, `file_hash_sha256` dedupe key, `doi` + OpenAlex taxonomy columns, `extraction_quality JSON`, unguarded `download_token`).
- `documents` fan out: `document_texts` (1:1, LONGTEXT markdown, `extraction_method`), `document_chunks` (indexed by `(document_id, chunk_index)`), `document_references` (citation metadata + `edge_type`).
- `documents(PAPER)` → `paper_sections` (assigned student, `section_order`, `content_tex`, `previous_content_tex` for rollback, `version` capped at 2, `content_md_cache`).
- Claims: `claims` (→ project, optional section, `claim_version` optimistic concurrency, soft-delete `active`) → `ai_suggestions` (version-bound, `PENDING/ACCEPTED/REJECTED/INVALIDATED`, `score_breakdown JSON`, relation, strength band) → `claim_evidence_mappings` (UNIQUE(claim_id, document_chunk_id), `ACTIVE/INACTIVE`, `review_status PENDING/VERIFIED/REJECTED`, `relation_override`).
- Sharing bridge: `project_documents` (UNIQUE(project_id, document_id)) lets instructors push collection sources into projects.
- Review loop: `feedback_requests` (status `PENDING/RETURNED/REVIEWED/REJECTED`) → `instructor_feedbacks` (line-scoped, answered/answer_content); `section_feedback` (inline comments).
- Bookkeeping: `system_notifications`, `export_jobs`, `audit_logs` (`old_value/new_value JSON`), `project_checkpoints` (`snapshot_json LONGTEXT`), `review_snapshots` (SHA-256 `input_fingerprint` result cache).

**Normalization assessment:** the relational core is heavily normalized (bridge tables, versioned child rows). Denormalization appears as JSON columns (`score_breakdown`, `extraction_quality`, checkpoint/audit snapshots) and one questionable redundancy: `documents.project_id` + `documents.collection_id` coexist with the `project_documents` bridge — sources live in both places (a project-owned document *and* a shared document), forcing `ClaimMatchingServiceImpl.activeSourceDocumentIds` and `isDocumentInProject` to union two lookups. Schema management is split three ways (`schema.sql` for fresh Docker init, Hibernate `ddl-auto=update` for evolution, `SchemaMigrationRunner` doing runtime ALTERs) — a known drift risk.

## 4. Crucial Business Logic & Workflows

### 4a. Document ingestion pipeline (the most complex flow)
Upload (multipart) → `PENDING_UPLOAD` → `markDocumentAsUploaded` → `DocumentUploadedEvent` → RabbitMQ `extraction.queue` → `DocumentExtractionWorkerImpl.process`:
1. **Checkpoint resume** — reads `documents/processed/{id}/extraction.json` from MinIO; retries reuse cached extraction.
2. **AI extraction** — posts filename + a tokenized download URL to the Python worker; validates ZIP content-type and a 100 MiB cap; imports extracted images to MinIO and rewrites `![](img)` → `\includegraphics{img}`.
3. **Chunking** (`DocumentChunker`) — 2000-char windows, ≤3 blocks, heading-prefix context, table-aware splitting, sentence-boundary fallback.
4. **Embedding** — 32/batch, hard-validated 768-dim dense + local `SparseVectorGenerator`; chunk rows upserted by index (stale chunks soft-deactivated).
5. **Qdrant upsert** per chunk (dense + sparse + payload) — **must succeed before READY**; failures → `FAILED` + broker retry.

### 4b. Claim-evidence matching & scoring
`POST /api/claims/{id}/matches/search` → embed claim → Qdrant top-20 filtered to the project's active SOURCE docs only. `POST .../suggestions/evaluate` runs a strict-JSON LLM prompt (`relation` ∈ `SUPPORTS|CONTRADICTS|NEUTRAL|EXTENDS|DETAILS|GENERALIZES` + explanation), then `EvidenceScoringService` computes a rubric-1.0 strength score (relation 35 + evidence anchor 20 + source authority 25 + citation metadata 10 + link availability 10 → HIGH≥70/MEDIUM≥40/LOW), guarded by a claim-version race check (HTTP 409 if the claim changed mid-evaluation). Accepting a suggestion materializes an `ACTIVE` mapping (`PENDING` review); editing a claim bumps `claim_version`, invalidates pending suggestions, and deactivates mappings.

### 4c. AI paper review
`POST /api/papers/{id}/review`: SHA-256 fingerprint (style + sections + claims + mappings + feedback) → `review_snapshots` cache; then per 8k-char chunk, two passes: (1) LLM findings strictly validated against UUID whitelists (deterministic types stripped from AI output), (2) assertion extraction → embedding cosine ≥ 0.7 → `MISSING_CLAIM`. Deterministic findings (UNUSED/ORPHANED/UNSUPPORTED/REDUNDANT) are computed in-Java.

### 4d. Project lifecycle & review loop
`submitForReview` (allowed only from `ASSIGNED/IN_PROGRESS/RETURNED`, requires all claims present, locks project, captures checkpoint) → instructor comments while `PENDING` → status transition maps to project state (`RETURNED`→student answers all feedback→`REVIEWED`→`APPROVED`; `REJECTED`→back to `SUBMITTED_FOR_REVIEW`). Every transition snapshots a checkpoint for the diff endpoint.

### 4e. Authentication / Authorization
Register (student-only, `PENDING`, hashed email token + TTL, SMTP) → verify → login (BCrypt, `email_verified` + `ACTIVE` checks) → JWT with `userId` + `tokenVersion` claims. `JwtAuthenticationFilter` validates signature, reloads the user per request, and rejects non-`ACTIVE` or token-version-mismatched sessions (instant ban/password-rotation kill). Authorization is **not** Spring-annotation based: every service method calls `CurrentUserService` (`requireProjectAccess/WriteAccess/ManageAccess`, `requireSectionAssignment`, `requireSectionContentWriteAccess`), where instructors get project access only as members or via an attached review request. Download endpoint is publicly accessible by unguessable token.

## 5. Integration & API Surface

- **~109 REST routes** under `/api` (auth 5, users 4, projects 17, claims 13, papers 17, documents 11, sources 4, collections 7, feedback 5, notifications 3, admin 11, exports 4, media 4, graph/progress/checkpoint/traceability, health, public/stats). Documented at `/swagger-ui.html`.
- **FE↔BE**: single axios instance (`VITE_API_BASE_URL`), Bearer token from `localStorage`, 401 → wipe + redirect. STOMP over `/ws` (`/user/queue`), plus aggressive polling for notifications and extraction status. `WorkspaceLayout.jsx` is the primary student workspace (sources, papers, claims, graph, format-scan, AI review, export, feedback).
- **Third-party**: Python AI worker (4 endpoints, optional `X-API-Key`, 660s read timeout), OpenAlex (DOI lookup/ingest), MinIO (presigned GET/PUT), SMTP. RabbitMQ is internal-only (extraction + export, publish deferred to `afterCommit`).

## 6. Current Technical Debt & Unresolved Architecture

1. **Scoring rubric is partially dead logic** — `EvidenceScoringService`: `source_type_authority` hardcodes 0/25 and `hasLocator` is always false; `evaluateMatch` passes `List.of()` references and `false` linkReachable, so citation/link components can never earn points. Max achievable strength ≈ 75/100 in the live path.
2. **Schema management triplication** — Flyway dependency present but disabled; `ddl-auto=update` mutates prod schema implicitly; `SchemaMigrationRunner` drops the *first* CHECK constraint it finds on `documents` (brittle ordering assumption); `spring.sql.init.mode=never` means new dev DBs must come from Docker init.
3. **Hardcoded vector contract** — 768-dim dense + `sparse` named vectors are hardcoded in `DocumentExtractionWorkerImpl`, `QdrantClientImpl`, and the `docker-compose` `qdrant-init` curl. Changing the embedding model requires a manual reindex; no versioning of collection schema.
4. **Two duplicate export paths** — legacy synchronous `GET /api/projects/{id}/export` (blob download used by `WorkspaceLayout`) coexists with the async `/api/exports` job flow; FE uses both.
5. **FE/BE contract drift & dead code** — legacy `Workspace.jsx` calls `/api/users/instructors` (no such endpoint → 404); `POST /api/paper/{documentId}/claims/match` (RagController) has no FE caller; `FE/src/mockData.js` is unreferenced; two competing student workspace implementations (`Workspace.jsx` vs `WorkspaceLayout.jsx`).
6. **Swallowed failures** (deliberate `ponytail:` shortcuts): checkpoint capture, review-snapshot save, and schema migration errors are logged-and-ignored — checkpoint/audit data loss is silent.
7. **AI determinism risk** — prompt versions (`paper-claim-review-v4`, `claim-evidence-v1`) and `ASSERTION_MATCH_THRESHOLD = 0.7` are constants awaiting eval-harness tuning; Qdrant TOP_K=20 fixed.
8. **Security surface** — Spring Security auto-config disabled with a hand-rolled filter chain; the `SecurityConfig` role matchers (`/api/users/**`) are vestigial since enforcement lives in services; raw `download_token` in URLs for the AI worker; JWT secret from env only (no rotation); tokens in `localStorage` (XSS exposure).
9. **No FE tests/lint**; BE has ~79 test files (H2 MySQL-mode) but the AI-dependent paths are only mocked-unit-tested.
